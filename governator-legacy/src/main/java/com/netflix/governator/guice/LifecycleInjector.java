/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.guice;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.annotation.Resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.guice.annotations.Bootstrap;
import com.netflix.governator.guice.lazy.FineGrainedLazySingleton;
import com.netflix.governator.guice.lazy.FineGrainedLazySingletonScope;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;

/**
 * <p>
 *     When using Governator, do NOT create a Guice injector manually. Instead, use a LifecycleInjector to create a Guice injector.
 * </p>
 *
 * <p>
 *     Governator uses a two pass binding. The bootstrap binding injects:
 *     <li>{@link LifecycleManager}</li>
 *     <li>Any application defined bootstrap instances</li>
 *     <br/>
 *     The main binding injects everything else.
 * </p>
 *
 * <p>
 *     The bootstrap binding occurs when the LifecycleInjector is created. The main binding
 *     occurs when {@link #createInjector()} is called.
 * </p>
 */
public class LifecycleInjector
{
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleInjector.class);
    
    private final ClasspathScanner scanner;
    private final List<Module> modules;
    private final Collection<Class<?>> ignoreClasses;
    private final boolean ignoreAllClasses;
    private final LifecycleManager lifecycleManager;
    private final Injector injector;
    private final Stage stage;
    private final LifecycleInjectorMode mode;
    private Set<PostInjectorAction> actions;
    private Set<ModuleTransformer> transformers;

    /**
     * Create a new LifecycleInjector builder
     *
     * @return builder
     */
    public static LifecycleInjectorBuilder builder()
    {
        return new LifecycleInjectorBuilderImpl();
    }
    
    public static Injector bootstrap(final Class<?> main) {
        return bootstrap(main, (Module)null);
    }
    
    public static Injector bootstrap(final Class<?> main, final BootstrapModule... externalBootstrapModules) {
        return bootstrap(main, null, externalBootstrapModules);
    }
    
    @SuppressWarnings("unused")
	private static BootstrapModule forAnnotation(final Annotation annot) {
        final Class<? extends Annotation> type = annot.annotationType();
    	return new BootstrapModule() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void configure(BootstrapBinder binder) {
	            binder.bind(Key.get(type))
	                .toProvider(new Provider() {
	                    @Override
	                    public Object get() {
	                        return annot;
	                    }
	                })
	                .in(Scopes.SINGLETON);
			}
    	};
    }
    
    /**
     * This is a shortcut to configuring the LifecycleInjectorBuilder using annotations.
     * 
     * Using bootstrap a main application class can simply be annotated with 
     * custom annotations that are mapped to {@link BootstrapModule}s.
     * Each annotations can then map to a subsystem or feature that is enabled on 
     * the main application class.  {@link BootstrapModule}s are installed in the order in which 
     * they are defined.
     * 
     * @see {@link Bootstrap}
     * @param main Main application bootstrap class
     * @param externalBindings Bindings that are provided externally by the caller to bootstrap.  These
     *        bindings are injectable into the BootstrapModule instances
     * @param externalBootstrapModules Optional modules that are processed after all the main class bootstrap modules
     * @return The created injector
     */
    @Beta
    public static Injector bootstrap(final Class<?> main, final Module externalBindings, final BootstrapModule... externalBootstrapModules) {
        
        final LifecycleInjectorBuilder builder = LifecycleInjector.builder();
        
        // Create a temporary Guice injector for the purpose of constructing the list of
        // BootstrapModules which can inject any of the bootstrap annotations as well as 
        // the externally provided bindings.
        // Creation order,
        // 1.  Construct all BootstrapModule classes
        // 2.  Inject external bindings into BootstrapModule instances
        // 3.  Create the bootstrap injector with these modules
        Injector injector = Guice.createInjector(new AbstractModule() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected void configure() {
                if (externalBindings != null)
                    install(externalBindings);
                
                Multibinder<BootstrapModule> bootstrapModules = Multibinder.newSetBinder(binder(), BootstrapModule.class);
                Multibinder<LifecycleInjectorBuilderSuite> suites = Multibinder.newSetBinder(binder(), LifecycleInjectorBuilderSuite.class);
                
                if (externalBootstrapModules != null) {
                    for (final BootstrapModule bootstrapModule : externalBootstrapModules) {
                        bootstrapModules
                            .addBinding()
                            .toProvider(new MemberInjectingInstanceProvider<BootstrapModule>(bootstrapModule));
                    }
                }

                // Iterate through all annotations of the main class and convert them into
                // their BootstrapModules
                for (final Annotation annot : main.getDeclaredAnnotations()) {
                    final Class<? extends Annotation> type = annot.annotationType();
                    LOG.info("Found bootstrap annotation {}", type.getName());
                    Bootstrap bootstrap = type.getAnnotation(Bootstrap.class);
                    if (bootstrap != null) {
                    	boolean added = false;
                    	// This is a suite
                    	if (!bootstrap.value().equals(Bootstrap.NullLifecycleInjectorBuilderSuite.class)) {
                            LOG.info("Adding Suite {}", bootstrap.bootstrap());
                    		suites
	                            .addBinding()
	                            .to(bootstrap.value())
	                            .asEagerSingleton();
                    		added = true;
                    	}
                    	// This is a bootstrap module
                    	if (!bootstrap.bootstrap().equals(Bootstrap.NullBootstrapModule.class)) {
                    		Preconditions.checkState(added==false, bootstrap.annotationType().getName() + " already added as a LifecycleInjectorBuilderSuite");
                    		added = true;
                            LOG.info("Adding BootstrapModule {}", bootstrap.bootstrap());
	                        bootstrapModules
	                            .addBinding()
	                            .to(bootstrap.bootstrap())
	                            .asEagerSingleton();
	                        // Make this annotation injectable into any plain Module
	                        builder.withAdditionalBootstrapModules(forAnnotation(annot));
                    	}
                    	// This is a plain guice module
                    	if (!bootstrap.module().equals(Bootstrap.NullModule.class)) {
                    		Preconditions.checkState(added==false, bootstrap.annotationType().getName() + " already added as a BootstrapModule");
                    		added = true;
                            LOG.info("Adding Module {}", bootstrap.bootstrap());
	                        builder.withAdditionalModuleClasses(bootstrap.module());
	                        // Make the annotation injectable into the module
	                        builder.withAdditionalBootstrapModules(forAnnotation(annot));
                    	}
                            
                        // Makes the annotation injectable into LifecycleInjectorBuilderSuite
                        bind(Key.get(type))
                            .toProvider(new Provider() {
                                @Override
                                public Object get() {
                                    return annot;
                                }
                            })
                            .in(Scopes.SINGLETON);
                    }
                }
            }
        });
        
        // First, give all LifecycleInjectorBuilderSuite's priority 
        Set<LifecycleInjectorBuilderSuite> suites = injector.getInstance(Key.get(new TypeLiteral<Set<LifecycleInjectorBuilderSuite>>() {}));
        for (LifecycleInjectorBuilderSuite suite : suites) {
        	suite.configure(builder);
        }
        
        // Next, install BootstrapModule's
        builder.withAdditionalBootstrapModules(injector.getInstance(Key.get(new TypeLiteral<Set<BootstrapModule>>() {})));
        
        // The main class is added last so it can override any bindings from the BootstrapModule's and LifecycleInjectorBuilderSuite's
        if (Module.class.isAssignableFrom(main)) {
            try {
                builder.withAdditionalModuleClasses(main);
            } catch (Exception e) {
                throw new ProvisionException(String.format("Failed to create module for main class '%s'", main.getName()), e);
            }
        }
        
        // Finally, create and return the injector
        return builder.build().createInjector();
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
        Injector childInjector;
        
        Collection<Module> localModules = modules;
        for (ModuleTransformer transformer  : transformers) {
            localModules = transformer.call(localModules);
        }
        //noinspection deprecation
        if ( mode == LifecycleInjectorMode.REAL_CHILD_INJECTORS )
        {
            childInjector = injector.createChildInjector(localModules);
        }
        else 
        {
            childInjector = createSimulatedChildInjector(localModules);
        }
        
        for (PostInjectorAction action : actions) {
            action.call(childInjector);
        }
        
        return childInjector;
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
        // Add the discovered modules FIRST.  The discovered modules
        // are added, and will subsequently be configured, in module dependency 
        // order which will ensure that any singletons bound in these modules 
        // will be created in the same order as the bind() calls are made.
        // Note that the singleton ordering is only guaranteed for 
        // singleton scope.
        List<Module> localModules = Lists.newArrayList();
        if ( additionalModules != null )
        {
            localModules.addAll(additionalModules);
        }
        
        localModules.addAll(modules);

        // Finally, add the AutoBind module, which will use classpath scanning
        // to creating singleton bindings.  These singletons will be instantiated
        // in an indeterminate order but are guaranteed to occur AFTER singletons
        // bound in any of the discovered modules.
        if ( !ignoreAllClasses )
        {
            Collection<Class<?>>    localIgnoreClasses = Sets.newHashSet(ignoreClasses);
            localModules.add(new InternalAutoBindModule(injector, scanner, localIgnoreClasses));
        }

        return createChildInjector(localModules);
    }
 
    LifecycleInjector(LifecycleInjectorBuilderImpl builder)
    {
        this.scanner = (builder.getClasspathScanner() != null) 
                ? builder.getClasspathScanner() 
                : createStandardClasspathScanner(builder.isDisableAutoBinding() ? Collections.<String>emptyList() : builder.getBasePackages());
                
        AtomicReference<LifecycleManager> lifecycleManagerRef = new AtomicReference<LifecycleManager>();
        InternalBootstrapModule internalBootstrapModule = new InternalBootstrapModule(
                ImmutableList.<BootstrapModule>builder()
                    .addAll(builder.getBootstrapModules())
                    .add(new LoadersBootstrapModule(scanner))
                    .add(new InternalAutoBindModuleBootstrapModule(scanner, builder.getIgnoreClasses()))
                    .build(),
                scanner, 
                builder.getStage(),
                builder.getLifecycleInjectorMode(),
                builder.getModuleListBuilder(),
                builder.getPostInjectorActions(),
                builder.getModuleTransformers(),
                builder.isDisableAutoBinding());
        
        injector = Guice.createInjector
        (
            builder.getStage(),
            internalBootstrapModule,
            new InternalLifecycleModule(lifecycleManagerRef)
        );
        
        this.mode = Preconditions.checkNotNull(internalBootstrapModule.getMode(), "mode cannot be null");
        this.stage = Preconditions.checkNotNull(internalBootstrapModule.getStage(), "stage cannot be null");
        this.ignoreAllClasses = internalBootstrapModule.isDisableAutoBinding();
        this.ignoreClasses = ImmutableList.copyOf(builder.getIgnoreClasses());
        
        this.actions = injector.getInstance(Key.get(new TypeLiteral<Set<PostInjectorAction>>() {}));
        this.transformers = injector.getInstance(Key.get(new TypeLiteral<Set<ModuleTransformer>>() {}));
        
        try {
            this.modules = internalBootstrapModule.getModuleListBuilder().build(injector);
        } catch (Exception e) {
            throw new ProvisionException("Unable to resolve list of modules", e);
        }
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        lifecycleManagerRef.set(lifecycleManager);
    }

    private Injector createSimulatedChildInjector(Collection<Module> modules)
    {
        AbstractModule parentObjects = new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bindScope(LazySingleton.class, LazySingletonScope.get());
                bindScope(FineGrainedLazySingleton.class, FineGrainedLazySingletonScope.get());
                
                // Manually copy bindings from the bootstrap injector to the simulated child injector.
                Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();
                for (Entry<Key<?>, Binding<?>> binding : bindings.entrySet()) {
                    Class<?> cls = binding.getKey().getTypeLiteral().getRawType();
                    if (   Module.class.isAssignableFrom(cls)
                        || Injector.class.isAssignableFrom(cls)
                        || Stage.class.isAssignableFrom(cls)
                        || Logger.class.isAssignableFrom(cls)
                        || java.util.logging.Logger.class.isAssignableFrom(cls)
                        ) {
                        continue;
                    }
                    Provider provider = binding.getValue().getProvider();
                    bind(binding.getKey()).toProvider(provider);
                }
            }
        };

        AtomicReference<LifecycleManager> lifecycleManagerAtomicReference = new AtomicReference<LifecycleManager>(lifecycleManager);
        InternalLifecycleModule internalLifecycleModule = new InternalLifecycleModule(lifecycleManagerAtomicReference);

        List<Module> localModules = Lists.newArrayList(modules);
        localModules.add(parentObjects);
        localModules.add(internalLifecycleModule);
        return Guice.createInjector(stage, localModules);
    }
}
