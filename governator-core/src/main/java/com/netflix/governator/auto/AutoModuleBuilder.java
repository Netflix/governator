package com.netflix.governator.auto;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;
import com.netflix.governator.DefaultLifecycleListener;
import com.netflix.governator.DefaultModule;
import com.netflix.governator.ElementsEx;
import com.netflix.governator.Governator;
import com.netflix.governator.LifecycleListener;
import com.netflix.governator.LifecycleShutdownSignal;
import com.netflix.governator.auto.annotations.Bootstrap;
import com.netflix.governator.auto.annotations.Conditional;
import com.netflix.governator.auto.annotations.ConditionalOnProfile;
import com.netflix.governator.auto.annotations.OverrideModule;

/**
 * AutoModule provides automatic module loading using plugable module finders combined
 * with conditional loading and optional overrides.  The power of AutoModule comes into 
 * play when taking a complex system of modules and running it within different profiles
 * where bindings must be overridden or extended based on various conditions such 
 * as properties, environment, operating system and existence of jars on the class path.
 * 
 * Note that the output of the builder is a standard Guice module.  It is therefore 
 * possible to install multiple AutoModuleBuilder constructed modules as well as any 
 * other type of module.
 * 
 * Usage
 * <pre>
 * {@code
      // This is your application main
      public static void main(String args[]) {
          Governator.createInjector( 
              AutoModuleBuilder
                  .forModule(new ApplicationModule())
                  .withProfile("production")
                  .build()
              .awaitTermination()
      }
      
      // Module to capture all the bindings for an application
      public static class ApplicationModule extends AbstractModule {
          protected void configure() {
              install(new SomeLibraryModule());
              ...
          }
      }
      
      // This module can be picked up by classpath scanning or via the service loader
      // Since the conditions match this module will be installed as an override.
      @ConditionalOnProfile({"production"}
      @OverrideModule(SomeLibraryModule.class)
      public static class SomeLibraryModuleOverride extends AbstractModule {
      }
   }
 * </pre>
 * 
 * The above is equivalent to,
 * <pre>
 * {@code
      public static void main(String args[]) {
          Governator.createInjector( 
              Modules.override(new ApplicationModule())
                     .with(new SomeLibraryModuleOverride());
      }
   }
   </pre>
 * 
 * While the above example may seem verbose and not be too impressive when using a small number of modules
 * this functionality is extremely helpful in an application with hundreds of modules, some transitive, 
 * where overrides and additional functionality is desired when running in different environments.
 * 
 * @author elandau
 *
 */
public final class AutoModuleBuilder  {
    private static final Logger LOG = LoggerFactory.getLogger(AutoModuleBuilder.class);
    
    private final Module         module;
    private Set<String>          profiles = new HashSet<>();
    private Module               bootstrapModule = Modules.EMPTY_MODULE;
    private List<ModuleListProvider> moduleProviders = new ArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger();
    
    public static AutoModuleBuilder forModule(Module module) {
        return new AutoModuleBuilder(module);
    }
    
    public static AutoModuleBuilder forModule(Module ...module) {
        return new AutoModuleBuilder(Modules.combine(module));
    }
    
    public AutoModuleBuilder(Module module) {
        this.module = module;
    }

    /**
     * Add a module finder such as a ServiceLoaderModuleFinder or ClassPathScannerModuleFinder
     * @param finder
     * @return
     */
    public AutoModuleBuilder withModuleFinder(ModuleListProvider finder) {
        this.moduleProviders.add(finder);
        return this;
    }
    
    /**
     * Bootstrap overrides for the bootstrap injector used to load and inject into 
     * the conditions.  Bootstrap does not restrict the bindings to allow any type
     * to be externally provided and injected into conditions.  Several simple
     * bindings are provided by default and may be overridden,
     * 1.  Config
     * 2.  Profiles
     * 3.  BoundKeys (TODO)
     * 
     * @param bootstrapModule
     */
    public AutoModuleBuilder withBootstrap(Module bootstrapModule) {
        this.bootstrapModule = bootstrapModule;
        return this;
    }
    
    public AutoModuleBuilder withBootstrap(Module ... bootstrapModule) {
        this.bootstrapModule = Modules.combine(bootstrapModule);
        return this;
    }

    /**
     * Add a runtime profile.  @see {@link ConditionalOnProfile}
     * 
     * @param profile
     */
    public AutoModuleBuilder withProfile(String profile) {
        this.profiles.add(profile);
        return this;
    }

    /**
     * Add a runtime profiles.  @see {@link ConditionalOnProfile}
     * 
     * @param profile
     */
    public AutoModuleBuilder withProfiles(String... profiles) {
        this.profiles.addAll(Arrays.asList(profiles));
        return this;
    }
    
    /**
     * Add a runtime profiles.  @see {@link ConditionalOnProfile}
     * 
     * @param profile
     */
    public AutoModuleBuilder withProfiles(Collection<String> profiles) {
        this.profiles.addAll(profiles);
        return this;
    }

    private String formatConditional(Annotation a) {
        String str = a.toString();
        int pos = str.indexOf("(");
        if (pos != -1) {
            pos = str.lastIndexOf(".", pos);
            if (pos != -1) {
                return str.substring(pos+1);
            }
        }
        return str;
    }
    
    private boolean evaluateConditions(Injector injector, Module module, boolean isBootstrap) throws Exception {
        LOG.info("Evaluating module {}", module.getClass().getName());
        
        Bootstrap bs = module.getClass().getAnnotation(Bootstrap.class);
        if (isBootstrap != (bs != null)) {
            LOG.info("  - ConditionalOn{}Bootstrap", isBootstrap ? "" : "Not");
            return false;
        }
        else {
            LOG.info("  + ConditionalOn{}Bootstrap", isBootstrap ? "" : "Not");
        }
        
        // The class may have multiple Conditional annotations
        for (Annotation annot : module.getClass().getAnnotations()) {
            Conditional conditional = annot.annotationType().getAnnotation(Conditional.class);
            if (conditional != null) {
                // A Conditional may have a list of multiple Conditions
                for (Class<? extends Condition> condition : conditional.value()) {
                    try {
                        // Construct the condition using Guice so that anything may be injected into 
                        // the condition
                        Condition c = injector.getInstance(condition);
                        // Look for method signature : boolean check(T annot)
                        // where T is the annotation type.  Note that the same checker will be used 
                        // for all conditions of the same annotation type.
                        try {
                            Method check = condition.getDeclaredMethod("check", annot.annotationType());
                            if (!(boolean)check.invoke(c, annot)) {
                                LOG.info("  - {}", formatConditional(annot));
                                return false;
                            }
                        }
                        // If not found, look for method signature 
                        //      boolean check();
                        catch (NoSuchMethodException e) {
                            Method check = condition.getDeclaredMethod("check");
                            if (!(boolean)check.invoke(c)) {
                                LOG.info("  - {}", formatConditional(annot));
                                return false;
                            }
                        }
                        
                        LOG.info("  + {}", formatConditional(annot));
                    }
                    catch (Exception e) {
                        LOG.info("  - {}", formatConditional(annot));
                        throw new Exception("Failed to check condition '" + condition + "' on module '" + module.getClass() + "'");
                    }
                }
            }
        }
        return true;
    }
    
    public Module build() {
        // If no loader has been specified use the default which is to load
        // all Module classes via the ServiceLoader
        if (moduleProviders.isEmpty()) {
            moduleProviders.add(new ServiceLoaderModuleProvider());
        }
        
        // Generate a single list of all found modules
        // TODO: Duplicates?
        final List<Module> loadedModules   = new ArrayList<>();
        for (ModuleListProvider loader : moduleProviders) {
            loadedModules.addAll(loader.get());
        }

        Module newBootstrap = create(loadedModules, bootstrapModule, profiles, true, new DefaultModule() {
            @Provides
            PropertySource getPropertySource() {
                return new DefaultPropertySource(); 
            }
        });
        return create(loadedModules, module, profiles, false, newBootstrap);
    }
    
    private Module create(List<Module> loadedModules, Module rootModule, final Set<String> profiles, final boolean isBootstrap, Module bootstrapModule) {
        LOG.info("Processing profiles : " + profiles);
        
        // Populate all the bootstrap state from the main module
        final List<Element> elements    = Elements.getElements(Stage.DEVELOPMENT, rootModule);
        final Set<Key<?>>   keys        = ElementsEx.getAllInjectionKeys(elements);
        final List<String>  moduleNames = ElementsEx.getAllSourceModules(elements);
        
        final Injector injector = Governator.createInjector(Modules
                .override(new DefaultModule() {
                    @Provides
                    public AutoContext getContext() {
                        return new AutoContext() {
                            @Override
                            public boolean hasProfile(String profile) {
                                return profiles.contains(profile);
                            }
        
                            @Override
                            public boolean hasModule(String className) {
                                return moduleNames.contains(className);
                            }
                            
                            @Override
                            public boolean hasBinding(Key<?> key) {
                                return keys.contains(key);
                            }
                        };
                    }
                })
                .with(bootstrapModule));

        // Iterate through all loaded modules and filter out any modules that
        // have failed the condition check.  Also, keep track of any override modules
        // for already installed modules.
        final List<Module> overrideModules = new ArrayList<>();
        final List<Module> moreModules     = new ArrayList<>();
        for (Module module : loadedModules) {
            try {
                if (evaluateConditions(injector, module, isBootstrap)) {
                    OverrideModule override = module.getClass().getAnnotation(OverrideModule.class);
                    if (override != null) {
                        if (moduleNames.contains(override.value().getName())) {
                            LOG.info("    Adding override module {}", module.getClass().getSimpleName());
                            overrideModules.add(module);
                        }
                    }
                    else {
                        LOG.info("    Adding conditional module {}", module.getClass().getSimpleName());
                        moreModules.add(module);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        List<Binding<BootstrapExposedModule>> bootstrapModules = injector.findBindingsByType(TypeLiteral.get(BootstrapExposedModule.class));
        for (Binding<BootstrapExposedModule> binding : bootstrapModules) {
            Module module = binding.getProvider().get();
            LOG.info("Adding exposed bootstrap module {}", module.getClass().getName());
            moreModules.add(module);
        }

        return Modules
            .override(new AbstractModule() {
                @Override
                protected void configure() {
                    binder().skipSources(getClass());
                    
                    install(Elements.getModule(elements));
                    install(Modules.combine(moreModules));
                    
                    // Slave the bootstrap module's shutdown the what injector is created here
                    // Otherwise it will not be possible to shutdown any lifecycle listeners
                    // from the bootstrap injector
                    final LifecycleShutdownSignal bootstrapLifecycleShutdownSignal = injector.getInstance(LifecycleShutdownSignal.class);
                    bind(LifecycleListener.class)
                        .annotatedWith(Names.named("BootstrapLifecycleBridge" + idCounter.incrementAndGet()))
                        .toInstance(new DefaultLifecycleListener() {
                            @Override
                            public void onStopped() {
                                bootstrapLifecycleShutdownSignal.signal();
                            }
                        });
                }
            })
            .with(overrideModules);
    }
    
}
