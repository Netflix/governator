package com.netflix.governator.guice;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.annotation.Resources;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;

/**
 * This is an alternative to {@link LifecycleInjector} which follows a slightly different
 * approach to lifecycle managements.
 * 
 * 1.  No implicit root injector.  createInjector() returns a root injector which helps
 *     resolve issues resulting from corner cases that use all of these concepts:
 *     JIT binding, Injecting the injector child injectors and Singletons.  
 * 2.  More emphasis on module driven bindings by providing module classes instead
 *     of module instances.  In instantiating the module classes it is possible to easily
 *     pull in dependent modules (specific as @Inject on the modules)
 * 
 * @author elandau
 *
 */
public class SimpleLifecycleInjector {
    public static class Builder
    {
        private List<Module>          modules            = Lists.newArrayList();
        private Collection<Class<?>>  ignoreClasses      = Lists.newArrayList();
        private Collection<String>    basePackages       = Lists.newArrayList();
        private boolean               ignoreAllClasses   = false;
        private List<BootstrapModule> bootstrapModules   = Lists.newArrayList();
        private ClasspathScanner      scanner            = null;
        private Stage                 stage              = Stage.PRODUCTION;
        private List<Class<? extends Module>> rootModules = Lists.newArrayList();

        public Builder withBootstrapModule(BootstrapModule module)
        {
            return withBootstrapModules(ImmutableList.of(module));
        }

        public Builder withBootstrapModules(BootstrapModule... additionalBootstrapModules) 
        {
            return withBootstrapModules(ImmutableList.copyOf(additionalBootstrapModules));
        }

        public Builder withBootstrapModules(Iterable<? extends BootstrapModule> additionalBootstrapModules) 
        {
            ImmutableList.Builder<BootstrapModule> builder = ImmutableList.builder();
            if ( this.bootstrapModules != null )
            {
                builder.addAll(this.bootstrapModules);
            }
            builder.addAll(additionalBootstrapModules);
            this.bootstrapModules = builder.build();
            return this;
        }

        public Builder withModules(Module... additionalModules) {
            return withModules(ImmutableList.copyOf(additionalModules));
        }
        
        public Builder withModules(Iterable<? extends Module> additionalModules)
        {
            ImmutableList.Builder<Module> builder = ImmutableList.builder();
            if ( this.modules != null )
            {
                builder.addAll(this.modules);
            }
            builder.addAll(additionalModules);
            this.modules = builder.build();
            return this;
        }

        public Builder withRootModule(Class<? extends Module> rootModule) 
        {
            this.rootModules.add(rootModule);
            return this;
        }
        
        public Builder ignoringAutoBindClasses(Collection<Class<?>> ignoreClasses)
        {
            this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
            return this;
        }

        public Builder ignoringAllAutoBindClasses()
        {
            this.ignoreAllClasses = true;
            return this;
        }

        public Builder usingBasePackages(String... basePackages)
        {
            return usingBasePackages(Arrays.asList(basePackages));
        }

        public Builder usingBasePackages(Collection<String> basePackages)
        {
            this.basePackages = Lists.newArrayList(basePackages);
            return this;
        }

        public Builder usingClasspathScanner(ClasspathScanner scanner)
        {
            this.scanner = scanner;
            return this;
        }

        public Builder inStage(Stage stage)
        {
            this.stage = stage;
            return this;
        }

        public SimpleLifecycleInjector build()
        {
            return new SimpleLifecycleInjector(this);
        }

    }
    
    
    /**
     * Create a new LifecycleInjector builder
     *
     * @return builder
     */
    public static Builder builder()
    {
        return new Builder();
    }

    
    private final ClasspathScanner      scanner;
    private final List<Module>          modules;
    private final Collection<Class<?>>  ignoreClasses;
    private final boolean               ignoreAllClasses;
    private LifecycleManager            lifecycleManager;
    private Injector                    injector;
    private final List<Module>          discoveredModules = Lists.newArrayList();
    private final List<BootstrapModule> bootstrapModules;
    private final Stage                 stage;
    
    SimpleLifecycleInjector(Builder builder)
    {
        this.stage = Preconditions.checkNotNull(builder.stage, "stage cannot be null");
        this.bootstrapModules = builder.bootstrapModules;
        this.ignoreAllClasses = builder.ignoreAllClasses;
        this.ignoreClasses = ImmutableList.copyOf(builder.ignoreClasses);
        this.modules = ImmutableList.copyOf(builder.modules);
        this.scanner = (builder.scanner != null) ? builder.scanner : createStandardClasspathScanner(builder.basePackages);
        
        if (!builder.rootModules.isEmpty()) {
            InternalModuleDependencyModule moduleDependencyModule = new InternalModuleDependencyModule();
            Injector tempInjector = Guice.createInjector(moduleDependencyModule);
            for (Class<? extends Module> rootModule : builder.rootModules) {
                tempInjector.getInstance(rootModule);
            }
            this.discoveredModules.addAll(moduleDependencyModule.getModules());
        }
    }

    /**
     * If you need early access to the CLASSPATH scanner. For performance reasons, you should
     * pass the scanner to the builder via {@link LifecycleInjectorBuilder#usingClasspathScanner(ClasspathScanner)}.
     *
     * @param basePackages packages to recursively scan
     * @return scanner
     */
    public static ClasspathScanner createStandardClasspathScanner(Collection<String> basePackages)
    {
        return createStandardClasspathScanner(basePackages, null);
    }

    /**
     * If you need early access to the CLASSPATH scanner. For performance reasons, you should
     * pass the scanner to the builder via {@link LifecycleInjectorBuilder#usingClasspathScanner(ClasspathScanner)}.
     *
     * @param basePackages packages to recursively scan
     * @param additionalAnnotations any additional annotations to scan for
     * @return scanner
     */
    public static ClasspathScanner createStandardClasspathScanner(Collection<String> basePackages, List<Class<? extends Annotation>> additionalAnnotations)
    {
        List<Class<? extends Annotation>> annotations = Lists.newArrayList();
        annotations.add(AutoBindSingleton.class);
        annotations.add(Inject.class);
        annotations.add(javax.inject.Inject.class);
        annotations.add(Resource.class);
        annotations.add(Resources.class);
        if ( additionalAnnotations != null )
        {
            annotations.addAll(additionalAnnotations);
        }
        return new ClasspathScanner(basePackages, annotations);
    }

    /**
     * Return the internally created lifecycle manager
     *
     * @return manager
     */
    public LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    /**
     * Create an injector that is a child of the bootstrap bindings only
     *
     * @param modules binding modules
     * @return injector
     */
    public Injector createChildInjector(Module... modules)
    {
        return createChildInjector(Arrays.asList(modules));
    }

    /**
     * Create an injector that is a child of the bootstrap bindings only
     *
     * @param modules binding modules
     * @return injector
     */
    public Injector createChildInjector(Collection<Module> modules)
    {
        return injector.createChildInjector(modules);
    }

    /**
     * Create the main injector
     *
     * @return injector
     */
    public Injector createInjector()
    {
        return createInjector(Lists.<Module>newArrayList());
    }

    /**
     * Create the main injector
     *
     * @param modules any additional modules
     * @return injector
     */
    public Injector createInjector(Module... modules)
    {
        return createInjector(Arrays.asList(modules));
    }

    /**
     * Create the main injector
     *
     * @param additionalModules any additional modules
     * @return injector
     */
    public Injector createInjector(Collection<Module> additionalModules)
    {
        Preconditions.checkArgument(injector == null, "Main injector can only be created once");
        
        AtomicReference<LifecycleManager> lifecycleManagerRef = new AtomicReference<LifecycleManager>();
        
        List<Module> localModules = Lists.newArrayList(
                new InternalLifecycleModule(lifecycleManagerRef),
                new InternalBootstrapModule(this.scanner, bootstrapModules)
                );
        
        // Add the discovered modules.  The discovered modules
        // are added, and will subsequently be configured, in module dependency 
        // order which will ensure that any singletons bound in these modules 
        // will be created in the same order as the bind() calls are made.
        // Note that the singleton ordering is only guaranteed for 
        // singleton scope.
        localModules.addAll(discoveredModules);
        localModules.addAll(modules);
        
        if ( additionalModules != null )
        {
            localModules.addAll(additionalModules);
        }

        // Finally, add the AutoBind module, which will use classpath scanning
        // to creating singleton bindings.  These singletons will be instantiated
        // in an indeterminate order but are guaranteed to occur AFTER singletons
        // bound in any of the discovered modules.
        if ( !ignoreAllClasses )
        {
            Collection<Class<?>>    localIgnoreClasses = Sets.newHashSet(ignoreClasses);
            localModules.add(new InternalAutoBindModule(
                Guice.createInjector(stage, new InternalBootstrapModule(this.scanner, bootstrapModules)), 
                scanner, localIgnoreClasses));
        }
        
        injector = Guice.createInjector
        (
            stage,
            localModules
        );
        
        lifecycleManager = lifecycleManagerRef.get();
        return injector;
    }
}
