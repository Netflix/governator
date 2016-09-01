package com.netflix.governator;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.spi.Elements;
import com.netflix.governator.spi.AnnotatedClassScanner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Set<String> packages = new HashSet<>();
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
    public ScanningModuleBuilder excludeClassesPredicate(Predicate<String> predicate) {
        excludeRule = Predicates.or(excludeRule, predicate);
        return this;
    }

    /**
     * Exclude specific classes from the classpath scanning.
     * @param classes Fully qualified classname to exclude
     * @return Builder for chaining
     */
    public ScanningModuleBuilder excludeClasses(String... classes) {
        return excludeClasses(Arrays.asList(classes));
    }
    
    /**
     * Exclude specific classes from the classpath scanning.
     * @param classes Fully qualified classname to exclude
     * @return Builder for chaining
     */
    public ScanningModuleBuilder excludeClasses(Collection<String> classes) {
        final Collection<String> toTest = new HashSet<>(classes);
        return excludeClassesPredicate(new Predicate<String>() {
           @Override
            public boolean apply(String className) {
               return toTest.contains(className);
            }
        });
    }
    
    private boolean isInTargetPackages(String pkgName) {
        if (packages.isEmpty()) {
            return true;
        }
        for ( String pkg : packages )  {
            if (pkgName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
    
    public Module build() {
        final Predicate<String> includeRule = Predicates.not(excludeRule);
        // Generate the list of elements here and immediately create a module from them.  This ensures
        // that the class path is canned only once as a Module's configure method may be called multiple
        // times by Guice.
        return Elements.getModule(Elements.getElements(new AbstractModule() {
            @Override
            public void configure() {
                final ClassPath classPath;
                try {
                    classPath = ClassPath.from(classLoader);
                } catch (IOException e) {
                    this.addError(e);
                    return;
                }
                
                for (ClassInfo classInfo : classPath.getAllClasses()) {
                    if (!isInTargetPackages(classInfo.getPackageName()) || !includeRule.apply(classInfo.getName())) {
                        continue;
                    }
                    
                    for (AnnotatedClassScanner scanner : scanners) {
                        Class<?> cls = classInfo.load();
                        if (cls.isAnnotationPresent(scanner.annotationClass())) {
                            try {
                                scanner.applyTo(binder(), cls.getAnnotation(scanner.annotationClass()), Key.get(cls));
                            } catch (Exception e) {
                                binder().addError("Failed process scanned class %s", classInfo.getName());
                                binder().addError(e);
                            }
                        }
                    }
                }
            }
        }));
    };
}
