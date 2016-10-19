package com.netflix.governator;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.netflix.governator.internal.scanner.ClasspathUrlDecoder;
import com.netflix.governator.spi.AnnotatedClassScanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
 *      .addScanner(new AutoBindSingletonAnnotatedClassScanner())
 *      .build()
 * }
 * </pre>
 */
public class ScanningModuleBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ScanningModuleBuilder.class);
    
    private Set<String> packages = new HashSet<>();
    private List<AnnotatedClassScanner> scanners = new ArrayList<>();
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private Predicate<Class<?>> excludeRule = (cls) -> false;
    
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
        this.packages = new HashSet<>(packages);
        return this;
    }
    
    /**
     * Specify an {@link AnnotatedClassScanner} to process classes from the set of specified packages.  
     * 
     * @param packages
     * @return Builder for chaining
     */
    public ScanningModuleBuilder addScanner(AnnotatedClassScanner scanner) {
        scanners.add(scanner);
        return this;
    }
    
    /**
     * Exclude bindings for any class matching the specified predicate.  Use this when implementing
     * custom exclude logic such as a regex match.
     * @param predicate Predicate that gets the fully qualified classname.
     * @return Builder for chaining
     */
    // TODO: Make this public when switching to Java8 and use the JDK's predicate
    private ScanningModuleBuilder excludeClassesWhen(Predicate<Class<?>> predicate) {
        excludeRule = excludeRule.or(predicate);
        return this;
    }

    /**
     * Exclude specific classes from the classpath scanning.
     * @param classes Classes to exclude
     * @return Builder for chaining
     */
    public ScanningModuleBuilder excludeClasses(Class<?>... classes) {
        return excludeClasses(new HashSet<>(Arrays.asList(classes)));
    }
    
    /**
     * Exclude specific classes from the classpath scanning.
     * @param packages Top packages to exclude
     * @return Builder for chaining
     */
    public ScanningModuleBuilder excludePackages(String... packages) {
        return excludeClassesWhen(cls -> {
            for (String pkg : packages) {
                if (cls.getPackage().getName().startsWith(pkg)) {
                    return true;
                };
            }
            return false;
        });
    }
    
    /**
     * Exclude specific classes from the classpath scanning.
     * @param classes Fully qualified classname to exclude
     * @return Builder for chaining
     */
    public ScanningModuleBuilder excludeClasses(Set<Class<?>> classes) {
        final Set<Class<?>> toTest = new HashSet<>(classes);
        return excludeClassesWhen(cls -> toTest.contains(cls));
    }
    
    public Module build() {
        final Predicate<Class<?>> includeRule = excludeRule.negate();

        List<Consumer<Binder>> consumers = new ArrayList<>();
        
        ScannerContext scanner = new ScannerContext();
        for ( String basePackage : packages )  {
            scanner.doScan(basePackage, new Consumer<String>() {
                @Override
                public void accept(String className) {
                    try {
                        Class<?> cls = Class.forName(className, false, classLoader);
                        if (includeRule.test(cls)) {
                            for (AnnotatedClassScanner scanner : scanners) {
                                if (cls.isAnnotationPresent(scanner.annotationClass())) {
                                    consumers.add(binder -> scanner.applyTo(binder, cls.getAnnotation(scanner.annotationClass()), Key.get(cls)));
                                }
                            }
                        }
                    } catch (ClassNotFoundException|NoClassDefFoundError e) {
                        LOG.debug("Error scanning class {}", className, e);
                    } catch (Error e) {
                        throw new RuntimeException("Error scanning class " + className, e);
                    }
                }
            });
        }
        
        // Generate the list of elements here and immediately create a module from them.  This ensures
        // that the class path is canned only once as a Module's configure method may be called multiple
        // times by Guice.
        return binder -> consumers.forEach(consumer -> consumer.accept(binder));
    }
    
    private class ScannerContext {
        // Used to dedup packages that were already scanned
        private final Set<URL> foundUrls = new HashSet<>();
        
        /**
         * Scan the specified packages and it's subpackages notifying the consumer for any new
         * class that's seen
         * @param basePackage
         * @param consumer
         * @throws Exception
         */
        void doScan(String basePackage, Consumer<String> consumer) {
            LOG.debug("Scanning package {}", basePackage);
            
            try {
                String basePackageWithSlashes = basePackage.replace(".", "/");
                for (URL url : Collections.list(classLoader.getResources(basePackageWithSlashes))) {
                    LOG.debug("Scanning url {}", url);
                    if (foundUrls.contains(url)) {
                        continue;
                    }
                    foundUrls.add(url);
                    
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
                                        int pos = entry.getName().indexOf(".class");
                                        if (pos != -1) {
                                            consumer.accept(entry.getName().substring(0,  pos).replace('/', '.'));
                                        }
                                    } catch (Exception e) {
                                        throw new Exception(
                                                String.format("Unable to scan JarEntry '%s' in '%s'. %s", entry.getName(), file.getCanonicalPath(), e.getMessage()));
                                    }
                                }
                            } catch (Exception e ) {
                                throw new Exception(String.format("Unable to scan '%s'. %s", file.getCanonicalPath(), e.getMessage()));
                            }
                        } else {
                            scanPackage(url, basePackage, consumer);
                        }
                    } catch (Exception e) {
                        throw new Exception(String.format("Unable to scan jar '%s'. %s ", url, e.getMessage()));
                    }
                }
            } catch ( Exception e ) {
                LOG.error("Classpath scanning failed for package '{}'", basePackage, e);
            }
        }
        
        private boolean isJarURL(URL url) {
            String protocol = url.getProtocol();
            return "zip".equals(protocol) || "jar".equals(protocol) ||
                    ("file".equals(protocol) && url.getPath().endsWith(".jar"));
        }
        
        private void scanPackage(URL url, String basePackage, Consumer<String> consumer) {
            File dir = ClasspathUrlDecoder.toFile(url);
            if (dir.isDirectory()) {
                scanDir(dir, (basePackage.length() > 0) ? (basePackage + ".") : "", consumer);
            }
        }

        private void scanDir(File dir, String packageName, Consumer<String> consumer) {
            LOG.debug("Scanning dir {}", packageName);
            
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDir(file, packageName + file.getName() + ".", consumer);
                } else if (file.getName().endsWith(".class")) {
                    String name = file.getName();
                    name = name.replaceFirst(".class$", "");
                    if (name.contains(".")) {
                        continue;
                    }
                    consumer.accept(packageName + name);
                }
            }
        }
    }
}
