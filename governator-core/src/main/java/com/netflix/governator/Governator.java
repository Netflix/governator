package com.netflix.governator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

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
        try {
            List<Module> l = new ArrayList<>(modules);
            l.add(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(LifecycleManager.class).toInstance(manager);
                }
            });
            
            Injector injector = Guice.createInjector(stage, l);
            return new LifecycleInjector(injector, manager);
        }
        catch (Exception e) {
            try {
                manager.shutdown();
            }
            catch (Exception e2) {
                
            }
            throw e;
        }
    }
}
