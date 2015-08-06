package com.netflix.governator.lifecycle;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import javax.tools.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaClasspath
{
    Map<String, byte[]> classpath = new HashMap<>();
    Map<String, File> jarByClass = new HashMap<>();

    Path temp;
    ClassLoader classLoader = byteClassLoader();

    public JavaClasspath()
    {
        try
        {
            temp = Files.createTempDirectory("classes");
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public void cleanup()
    {
        temp.toFile().delete();
    }

    private ClassLoader byteClassLoader()
    {
        return new ClassLoader()
        {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException
            {
                byte[] b = classpath.get(name);
                if(b == null)
                    throw new ClassNotFoundException(name);
                return defineClass(name, b, 0, b.length);
            }

            @Override
            protected Enumeration<URL> findResources(String name) throws IOException {
                Set<URL> matchingJars = new HashSet<>();
                for (Map.Entry<String, File> classToJar : jarByClass.entrySet()) {
                    if(classToJar.getKey().startsWith(name))
                        matchingJars.add(classToJar.getValue().toURI().toURL());
                }
                return Collections.enumeration(matchingJars);
            }
        };
    }

    private static String classNameFromSource(CharSequence source)
    {
        Matcher m = Pattern.compile("(class|interface)\\s+(\\w+)").matcher(source);
        return m.find() ? m.group(2) : null;
    }

    public <T> Class<T> loadClass(String className)
    {
        try
        {
            return (Class<T>) classLoader.loadClass(className);
        }
        catch(ClassNotFoundException e)
        {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T newInstance(String className, Object... args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException
    {
        Class<T> clazz = (Class<T>) loadClass(className);

        nextConstructor: for (Constructor<?> constructor : clazz.getDeclaredConstructors())
        {
            if(args.length != constructor.getParameterTypes().length)
                continue;
            int i = 0;
            for (Class type : constructor.getParameterTypes())
                if(!type.isAssignableFrom(args[i++].getClass()))
                    continue nextConstructor;

            constructor.setAccessible(true);
            try
            {
                return (T) constructor.newInstance(args);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e); // should never happen
            }
        }

        throw new IllegalArgumentException("No matching constructor for class " + className + " found for provided args");
    }

    private static class InMemoryJavaFileObject extends SimpleJavaFileObject
    {
        private String contents = null;

        public InMemoryJavaFileObject(String contents)
        {
            super(URI.create("string:///" + classNameFromSource(contents) + ".java"), Kind.SOURCE);
            this.contents = contents;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException
        {
            return contents;
        }
    }

    private static DiagnosticListener<JavaFileObject> diagnosticListener = new DiagnosticListener<JavaFileObject>()
    {
        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic)
        {
            System.out.println("ERROR compiling " + diagnostic.getSource().getName());
            System.out.println("Line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(Locale.ENGLISH));
        }
    };

    /**
     * Compiles all java sources and adds them to the rule's classpath
     * @param javaSources
     * @return the set of fully qualified class names compiled just in this invocation
     */
    public Collection<String> compile(String... javaSources)
    {
        return compile(Arrays.asList(javaSources));
    }

    /**
     * Compiles all java sources and adds them to the rule's classpath
     * @param javaSources
     * @return the set of fully qualified class names compiled just in this invocation
     */
    public Collection<String> compile(Collection<String> javaSources)
    {
        Collection<InMemoryJavaFileObject> files = new ArrayList<>();
        for (String javaSource : javaSources)
            files.add(new InMemoryJavaFileObject(javaSource));
        return compileInternal(files);
    }

    private Collection<String> compileInternal(Collection<InMemoryJavaFileObject> files)
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticListener, Locale.ENGLISH, null);

        try
        {
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticListener,
                    Arrays.asList("-d", temp.toFile().getAbsolutePath(), "-cp", temp.toFile().getAbsolutePath(), "-g"), null, files);
            task.call();

            Map<String, byte[]> classes = new HashMap<>();
            for (Path p : recurseListFiles(temp))
            {
                try
                {
                    byte[] bytes = Files.readAllBytes(p);
                    classes.put(fullyQualifiedName(bytes), bytes);
                }
                catch (IOException e)
                {
                    throw new IllegalStateException(e);
                }
            }

            classpath.putAll(classes);

            Set<String> classNames = new HashSet<>();
            classIter: for (String c : classes.keySet())
            {
                String[] classNameParts = c.split("\\.");
                for (InMemoryJavaFileObject file : files)
                {
                    if(classNameFromSource(file.contents).equals(classNameParts[classNameParts.length - 1]))
                    {
                        classNames.add(c);
                        continue classIter;
                    }
                }
            }

            return classNames;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private List<Path> recurseListFiles(Path path) throws IOException
    {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path))
        {
            for (Path entry : stream)
            {
                if (Files.isDirectory(entry))
                    files.addAll(recurseListFiles(entry));
                else
                    files.add(entry);
            }
        }
        return files;
    }

    public String fullyQualifiedName(byte[] classBytes)
    {
        final StringBuffer className = new StringBuffer();

        ClassReader cr = new ClassReader(classBytes);
        cr.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
            {
                className.append(name);
            }
        }, 0);

        return className.toString().replace("/", ".");
    }

    public File jar(File f, String... classSources)
    {
        f.getParentFile().mkdirs();

        try
        {
            FileOutputStream fos = new FileOutputStream(f);
            JarOutputStream jos = new JarOutputStream(fos);

            for (String clazz : compile(classSources))
            {
                jos.putNextEntry(new JarEntry(clazz.replace(".", "/") + ".class"));
                jos.write(classpath.get(clazz));
                jarByClass.put(clazz.replace('.', '/'), f);
            }

            jos.close();
            fos.close();

            return f;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public byte[] classBytes(String className)
    {
        return classpath.get(className);
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }
}
