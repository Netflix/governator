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

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;
import com.netflix.governator.auto.AutoContext;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.ModuleListProvider;
import com.netflix.governator.auto.PropertySource;
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
                
                @Override
                public String toString() {
                    return "LifecycleManager binding";
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
     * 
     *      +-------------------+
     *      |      Override     |
     *      +-------------------+
     *      |   Auto Override   |
     *      +-------------------+
     *      |    Core + Auto    |
     *      +-------------------+
     *      | Bootstrap Exposed |
     *      +-------------------+
     *      
     *      
     * @return
     */
    public static LifecycleInjector createInjector(final GovernatorConfiguration config) {
        Logger LOG = LoggerFactory.getLogger(Governator.class);
        LOG.info("Using profiles : " + config.getProfiles());
        
        // Load all candidate modules for auto-loading/override
        final Set<Module> candidateModules   = new HashSet<>();
        for (ModuleListProvider loader : config.getModuleListProviders()) {
            candidateModules.addAll(loader.get());
        }
        
        // Create the main LifecycleManager to be used by all levels
        final LifecycleManager manager = new LifecycleManager();
        
        // Construct the injector using our override structure
        try {
            Module coreModule = Modules.override(
                    createAutoModule(LOG, config, candidateModules, config.getModules()))
                   .with(config.getOverrideModules());
            
            for (Element binding : Elements.getElements(coreModule)) {
                LOG.debug("Binding : {}", binding);
            }
            
            LOG.info("Configured override modules : " + config.getOverrideModules());
            
            Injector injector = Guice.createInjector(
                    config.getStage(),
                    new LifecycleModule(),
                    new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(LifecycleManager.class).toInstance(manager);
                            bind(GovernatorConfiguration.class).toInstance(config);
                            bind(PropertySource.class).toInstance(config.getPropertySource());
                        }
                    },
                    coreModule
                    );
            manager.notifyStarted();
            return new LifecycleInjector(injector, manager);
        }
        catch (Throwable e) {
            e.printStackTrace(System.err);
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
    }
    
    private static Module createAutoModule(final Logger LOG, final GovernatorConfiguration config, final Set<Module> candidateModules, final List<Module> coreModules) throws Exception {
        LOG.info("Creating {} injector");
        final List<Element> elements    = Elements.getElements(Stage.DEVELOPMENT, coreModules);
        final Set<Key<?>>   keys        = ElementsEx.getAllInjectionKeys(elements);
        final List<String>  moduleNames = ElementsEx.getAllSourceModules(elements);
        
        final AutoContext context = new AutoContext() {
            @Override
            public boolean hasModule(String className) {
                return moduleNames.contains(className);
            }

            @Override
            public boolean hasProfile(String profile) {
                return config.getProfiles().contains(profile);
            }

            @Override
            public boolean hasBinding(Key<?> key) {
                return keys.contains(key);
            }
            
            @Override
            public List<Element> getElements() {
                return elements;
            }
        };
        
        // Temporary injector to used to construct the condition checks
        final Injector injector = Guice.createInjector(config.getStage(), 
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(GovernatorConfiguration.class).toInstance(config);
                    bind(PropertySource.class).toInstance(config.getPropertySource());
                    bind(AutoContext.class).toInstance(context);
                }
            });
        
        PropertySource propertySource = config.getPropertySource();
        
        // Iterate through all loaded modules and filter out any modules that
        // have failed the condition check.  Also, keep track of any override modules
        // for already installed modules.
        final List<Module> overrideModules = new ArrayList<>();
        final List<Module> autoModules     = new ArrayList<>();
        for (Module module : candidateModules) {
            if (!isModuleEnabled(propertySource, module)) {
                LOG.info("(IGNORING) {}", module.getClass().getName());
                continue;
            }
            
            if (shouldInstallModule(LOG, injector, module)) {
                OverrideModule override = module.getClass().getAnnotation(OverrideModule.class);
                if (override != null) {
                    LOG.info("  (ADDING) {}", module.getClass().getSimpleName());
                    overrideModules.add(module);
                }
                else {
                    LOG.info("  (ADDING) {}", module.getClass().getSimpleName());
                    autoModules.add(module);
                }
            }
            else {
                LOG.info("  (DISCARD) {}", module.getClass().getSimpleName());
            }
        }
        
        LOG.info("Core Modules     : " + coreModules);
        LOG.info("Auto Modules     : " + autoModules);
        LOG.info("Override Modules : " + overrideModules);
        
        return Modules.override(ImmutableList.<Module>builder().addAll(coreModules).addAll(autoModules).build())
                      .with(overrideModules);
    }

    /**
     * Determine if a module should be installed based on the conditional annotations
     * @param LOG
     * @param injector
     * @param module
     * 
     * @return
     * @throws Exception
     */
    private static boolean shouldInstallModule(Logger LOG, Injector injector, Module module) throws Exception {
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
                                LOG.info("  FAIL {}", formatConditional(annot));
                                return false;
                            }
                        }
                        // If not found, look for method signature 
                        //      boolean check();
                        catch (NoSuchMethodException e) {
                            Method check = condition.getDeclaredMethod("check");
                            if (!(boolean)check.invoke(c)) {
                                LOG.info("  FAIL {}", formatConditional(annot));
                                return false;
                            }
                        }
                        
                        LOG.info("  (PASS) {}", formatConditional(annot));
                    }
                    catch (Exception e) {
                        LOG.info("  (FAIL) {}", formatConditional(annot), e);
                        throw new Exception("Failed to check condition '" + condition + "' on module '" + module.getClass() + "'", e);
                    }
                }
            }
        }
        return true;
    }
    
    private static Boolean isModuleEnabled(final PropertySource propertySource, final Module module) {
        String name = module.getClass().getName();
        int pos = name.length();
        do {
            if (propertySource.get("governator.module.disabled." + name.substring(0, pos), Boolean.class, false)) {
                return false;
            }
            pos = name.lastIndexOf(".", pos-1);
        } while (pos > 0);
        return true;
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
}
