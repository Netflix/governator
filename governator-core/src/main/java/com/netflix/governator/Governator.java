package com.netflix.governator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;
import com.netflix.governator.auto.AutoContext;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.DefaultPropertySource;
import com.netflix.governator.auto.ModuleListProvider;
import com.netflix.governator.auto.ModuleProvider;
import com.netflix.governator.auto.PropertySource;
import com.netflix.governator.auto.annotations.Bootstrap;
import com.netflix.governator.auto.annotations.Conditional;
import com.netflix.governator.auto.annotations.OverrideModule;

/**
 * Utility class matching Guice's {@link Guice} but providing shutdown capabilities.
 * Note that the injector being created will not by default support @PreDestory and
 * @PostConstruct.  Those are supported by adding LifecycleModule to the list of modules.
 * 
 * @author elandau
 *
 */
public class Governator {
    public static LifecycleInjector createInjector() {
        return createInjector(Stage.PRODUCTION, Collections.<Module>emptyList());
    }
    
    public static LifecycleInjector createInjector(Module ... modules) {
        return createInjector(Stage.PRODUCTION, modules);
    }

    public static LifecycleInjector createInjector(Stage stage, Module ... modules) {
        return createInjector(stage, Arrays.asList(modules));
    }

    public static LifecycleInjector createInjector(Collection<? extends Module> modules) {
        return createInjector(Stage.PRODUCTION, modules);
    }
    
    public static LifecycleInjector createInjector(Stage stage, Collection<? extends Module> modules) {
        final LifecycleManager manager = new LifecycleManager();
        Injector injector;
        List<Module> l = new ArrayList<>();
        try {
            l.add(new LifecycleModule());
            l.add(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(LifecycleManager.class).toInstance(manager);
                    requestInjection(manager);
                }
            });
            l.addAll(modules);
            
            injector = Guice.createInjector(stage, l);
        }
        catch (Exception e) {
            try {
                manager.notifyStartFailed(e);
            }
            catch (Exception e2) {
                System.err.println("Failed to notify LifecycleManager");
                e.printStackTrace();
            }
            throw e;
        }
        
        try {
            manager.notifyStarted();
            return new LifecycleInjector(injector, manager);
        }
        catch (Exception e) {
            manager.notifyShutdown();
            throw e;
        }
    }
    
    /**
     * Entry point for creating a LifecycleInjector with module auto-loading capabilities.  
     * Module auto-loading makes it possible to load bindings that are contextual to the 
     * environment in which the application is running based on things like profiles,
     * properties and existing bindings.  
     * 
     * The LifecycleInjector created here uses a layered approach to construct the Guice Injector
     * so that bindings can be overridden at a high level based on the runtime environment
     * as opposed to sprinkling Modules.overrides and conditionals throughout the modules themselves.
     * Using Modules.overrides directly looses the original module's context and can easily result
     * in difficult to debug duplicate binding errors.
     * 
     * This injector is constructed in two phases.  The first bootstrap phase determines which core
     * Guice modules should be installed based on processing of conditional annotations.  The final
     * list of auto discovered modules is appended to the main list of modules and installed on the
     * main injector.  Application level override modules may be applied to this final list from the
     * list of modules returned from {@link GovernatorConfiguration.getOverrideModules()}.
     * 
     * <pre>
     * {@code
       Governator.createInjector(
             DefaultGovernatorConfiguration().builder()
                .addModules(
                     new JettyModule(),
                     new JerseyServletModule() {
                        @Override
                        protected void configureServlets() {
                            serve("/*").with(GuiceContainer.class);
                            bind(GuiceContainer.class).asEagerSingleton();
                            
                            bind(HelloWorldApp.class).asEagerSingleton();
                        }  
                    }
                )
                .build()
            )
            .awaitTermination();
     * }
     * </pre>
     * @param config
     * @param modules
     * @return
     */
    public static LifecycleInjector createInjector(final GovernatorConfiguration config) {
        // The logger is intentionally created here to avoid early static initialization
        // of SLF4J/LOG4J which may be customized using one of the bootstrap modules
        Logger LOG = LoggerFactory.getLogger(Governator.class);
        LOG.info("Using profiles : " + config.getProfiles());
        
        // Generate a single list of all discovered modules
        // TODO: Duplicates?
        final Set<Module> loadedModules   = new HashSet<>();
        for (ModuleListProvider loader : config.getModuleListProviders()) {
            loadedModules.addAll(loader.get());
        }

        final LifecycleManager manager = new LifecycleManager();
        
        Injector injector;
        try {
            Module modules = Modules.combine(
                new LifecycleModule(), 
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(GovernatorConfiguration.class).toInstance(config);
                        bind(LifecycleManager.class).toInstance(manager);
                        requestInjection(manager);
                    }
                }, 
                create(
                    LOG,
                    config,
                    manager,
                    loadedModules, 
                    config.getModules(), 
                    false, 
                    // First, auto load the bootstrap modules (usually deal with configuration and logging) and
                    // use to load the main module.
                    create(
                        LOG,
                        config,
                        manager,
                        loadedModules, 
                        config.getBootstrapModules(), 
                        true, 
                        new DefaultModule() {
                            @Provides
                            PropertySource getPropertySource() {
                                return new DefaultPropertySource(); 
                            }
                        })));
            if (!config.getOverrideModules().isEmpty()) {
                modules = Modules.override(modules).with(config.getOverrideModules());
            }
                    
            injector = Guice.createInjector(config.getStage(), modules);
        }
        catch (Throwable e) {
            e.printStackTrace();
            try {
                manager.notifyStartFailed(e);
            }
            catch (Exception e2) {
                System.err.println("Failed to notify injector creation failure!");
                e2.printStackTrace(System.err);
            }
            if (config.isEnabled(GovernatorFeatures.SHUTDOWN_ON_ERROR))
                throw new RuntimeException(e);
            return new LifecycleInjector(null, manager);
        }
        
        try {
            manager.notifyStarted();
            return new LifecycleInjector(injector, manager);
        }
        catch (Exception e) {
            manager.notifyShutdown();
            throw e;
        }
    }
    
    private static String formatConditional(Annotation a) {
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
    
    private static boolean evaluateConditions(Logger LOG, Injector injector, Module module) throws Exception {
        LOG.info("Evaluating module {}", module.getClass().getName());
        
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
    
    private static boolean isEnabled(PropertySource propertySource, String name) {
        int pos = name.length();
        do {
            if (propertySource.get("governator.module.disabled." + name.substring(0, pos), Boolean.class, false)) {
                return false;
            }
            pos = name.lastIndexOf(".", pos-1);
        } while (pos > 0);
        return true;
    }
    
    private static Module create(final Logger LOG, final GovernatorConfiguration config, final LifecycleManager manager, final Collection<Module> loadedModules, final List<Module> rootModules, final boolean isBootstrap, final Module bootstrapModule) throws Exception {
        LOG.info("Creating {} injector", isBootstrap ? "bootstrap" : "main");
        // Populate all the bootstrap state from the main module
        final List<Element> elements    = Elements.getElements(Stage.DEVELOPMENT, rootModules);
        final Set<Key<?>>   keys        = ElementsEx.getAllInjectionKeys(elements);
        final List<String>  moduleNames = ElementsEx.getAllSourceModules(elements);
        
        final Injector injector = Guice.createInjector(
            config.getStage(), 
            new LifecycleModule(), 
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(GovernatorConfiguration.class).toInstance(config);
                    bind(LifecycleManager.class).toInstance(manager);
                    requestInjection(manager);
                }
            }, 
            Modules
                .override(new DefaultModule() {
                    @Provides
                    public AutoContext getContext() {
                        return new AutoContext() {
                            @Override
                            public boolean hasProfile(String profile) {
                                return config.getProfiles().contains(profile);
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

        PropertySource propertySource = injector.getInstance(PropertySource.class);
        
        // Iterate through all loaded modules and filter out any modules that
        // have failed the condition check.  Also, keep track of any override modules
        // for already installed modules.
        final List<Module> overrideModules = new ArrayList<>();
        final List<Module> moreModules     = new ArrayList<>();
        for (Module module : loadedModules) {
            if (!isEnabled(propertySource, module.getClass().getName())) {
                LOG.info("Ignoring module {}", module.getClass().getName());
                continue;
            }
            
            Bootstrap bs = module.getClass().getAnnotation(Bootstrap.class);
            if (isBootstrap == (bs != null) && evaluateConditions(LOG, injector, module)) {
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
        }
        
        final List<Module> extModules     = new ArrayList<>();
        List<Binding<ModuleProvider>> moduleProviders = injector.findBindingsByType(TypeLiteral.get(ModuleProvider.class));
        for (Binding<ModuleProvider> binding : moduleProviders) {
            Module module = binding.getProvider().get().get();
            LOG.debug("Adding exposed bootstrap module {}", module.getClass().getName());
            extModules.add(module);
        }

        LOG.debug("Root Modules     : " + rootModules);
        LOG.debug("More Modules     : " + moreModules);
        LOG.debug("Override Modules : " + overrideModules);
        LOG.debug("Ext Modules      : " + extModules);
        
        LOG.debug("Created {} injector", isBootstrap ? "bootstrap" : "main");
        
        Module m = Modules
            .override(new AbstractModule() {
                @Override
                protected void configure() {
                    install(Modules.combine(rootModules));
                    install(Modules.combine(moreModules));
                }
            })
            .with(Modules
                .override(overrideModules)
                .with(Modules.combine(extModules)))
            ;
        return m;
    }
}
