package com.netflix.governator;

import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.Type.getType;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.netflix.governator.internal.scanner.ClasspathUrlDecoder;
import com.netflix.governator.internal.scanner.DirectoryClassFilter;
import com.netflix.governator.spi.AnnotatedClassScanner;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * When installed this module will scan for annotated classes and creating appropriate bindings.  
 * The specific annotation to scan and binding semantics are captured in a {@link AnnotatedClassScanner}.
 * 
 * The following example shows how to install a module that creates bindings for classes containing
 * the AutoBindSingleton annotation.
 * 
 * <pre>
 * {@code 
 * install(new ScanningModuleBuilder().
 *      .forPackages("org.example")
 *      .scanForAnnotatedClasses(new AutoBindSingletonAnnotatedClassScanner())
 *      .build()
 * }
 * </pre>
 */
public class ScanningModuleBuilder {
    private List<String> packages = new ArrayList<>();
    private List<AnnotatedClassScanner> scanners = new ArrayList<>();
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private Predicate<String> excludeRule = Predicates.alwaysFalse();
    
    /**
     * Specify a custom class loader to use.  If not specified Thread.currentThread().getContextClassLoader()
     * will be used.
     * 
     * @param classLoader
     * @return Builder for chaining
     */
    public ScanningModuleBuilder usingClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }
    
    /**
     * Set of packages to scan.  
     * 
     * @param packages
     * @return Builder for chaining
     */
    public ScanningModuleBuilder forPackages(String... packages) {
        return forPackages(Arrays.asList(packages));
    }
    
    /**
     * Set of packages to scan.  
     * 
     * @param packages
     * @return Builder for chaining
     */
    public ScanningModuleBuilder forPackages(Collection<String> packages) {
        this.packages = new ArrayList<>(packages);
        return this;
    }
    
    /**
     * Specify an {@link AnnotatedClassScanner} to process classes from the set of specified packages.  
     * 
     * @param packages
     * @return Builder for chaining
     */
    public ScanningModuleBuilder scanForAnnotatedClasses(AnnotatedClassScanner scanner) {
        scanners.add(scanner);
        return this;
    }
    
    /**
     * Exclude bindings for any class matching the specified predicate.
     * @param classes
     * @return Builder for chaining
     */
    public ScanningModuleBuilder excludeClasses(Predicate<String> predicate) {
        excludeRule = Predicates.or(excludeRule, predicate);
        return this;
    }

    /**
     * Exclude specific classes from the classpath scanning.
     * @param classes
     * @return Builder for chaining
     */
    public ScanningModuleBuilder excludeClasses(String... classes) {
        return excludeClasses(Arrays.asList(classes));
    }
    
    /**
     * Exclude specific classes from the classpath scanning.
     * @param classes
     * @return Builder for chaining
     */
    public ScanningModuleBuilder excludeClasses(Collection<String> classes) {
        final Collection<String> toTest = new HashSet<>(classes);
        return excludeClasses(new Predicate<String>() {
           @Override
            public boolean apply(String className) {
               return toTest.contains(className);
            }
        });
    }
    
    public Module build() {
        return build(classLoader, new ArrayList<>(scanners), new ArrayList<>(packages), Predicates.not(excludeRule));
    }
    
    private Module build(final ClassLoader classLoader, final List<AnnotatedClassScanner> scanners, final List<String> packages, final Predicate<String> includeRule) {
        return new AbstractModule() {
            @Override
            public void configure() {
                for ( String basePackage : packages )  {
                    try {
                        String basePackageWithSlashes = basePackage.replace(".", "/");
                        for (URL url : Collections.list(classLoader.getResources(basePackageWithSlashes))) {
                            try {
                                if ( isJarURL(url)) {
                                    String jarPath = url.getFile();
                                    if ( jarPath.contains("!") ) {
                                        jarPath = jarPath.substring(0, jarPath.indexOf("!"));
                                        url = new URL(jarPath);
                                    }
                                    File file = ClasspathUrlDecoder.toFile(url);
                                    try (JarFile jar = new JarFile(file)) {
                                        for (JarEntry entry : Collections.list(jar.entries())) {
                                            try {
                                                if ( entry.getName().endsWith(".class") && entry.getName().startsWith(basePackageWithSlashes)) {
                                                    try (InputStream is = jar.getInputStream(entry)) {
                                                        handleClass(is);
                                                    }
                                                }
                                            } catch (Exception e) {
                                                addError("Unable to scan JarEntry '%s' in '%s'. %s", entry.getName(), file.getCanonicalPath(), e.getMessage());
                                                addError(e);
                                            }
                                        }
                                    } catch (Exception e ) {
                                        addError("Unable to scan '%s'. %s", file.getCanonicalPath(), e.getMessage());
                                        addError(e);
                                    }
                                } else {
                                    DirectoryClassFilter filter = new DirectoryClassFilter(classLoader);
                                    for ( String className : filter.filesInPackage(url, basePackage) ) {
                                        try (InputStream is = filter.bytecodeOf(className)) {
                                            handleClass(is);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                addError("Unable to scan jar '%s'. %s ", url, e.getMessage());
                                addError(e);
                            }
                        }
                    } catch ( Exception e ) {
                        addError("Classpath scanning failed for package \'" + basePackage + "\'");
                        addError(e);
                    }
                }
            }
            
            private boolean isJarURL(URL url) {
                String protocol = url.getProtocol();
                return "zip".equals(protocol) || "jar".equals(protocol) ||
                        ("file".equals(protocol) && url.getPath().endsWith(".jar"));
            }
            
            private void handleClass(InputStream inputStream) throws IOException {
                new ClassReader(inputStream).accept(new ClassVisitor(Opcodes.ASM5) {
                    private String className;
                    
                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        className = name.replace('/', '.');
                        super.visit(version, access, name, signature, superName, interfaces);
                    }
                    
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        Type type = getType(desc);
                        if (includeRule.apply(className)) {
                            for (AnnotatedClassScanner scanner : scanners) {
                                if (getType(scanner.annotationClass()).equals(type)) {
                                    try {
                                        Class<?> cls = Class.forName(className, false, classLoader);
                                        scanner.applyTo(binder(), cls.getAnnotation(scanner.annotationClass()), Key.get(cls));
                                    } catch (ClassNotFoundException e) {
                                        binder().addError(e);
                                        binder().addError("Failed process scanned class %s", className);
                                    }
                                }
                            }
                        }
                        
                        return super.visitAnnotation(desc, visible);
                    }
                }, SKIP_CODE);
            }
        };
    };
}
