package com.netflix.governator.guice;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * This module is used by Governator to build a dependency ordered list of 
 * modules in the bootstrap injector.  This list of modules will then be 
 * added to the main child injector.
 * 
 * @author elandau
 *
 */
public class internalModuleDependencyModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(internalModuleDependencyModule.class);
    
    private final List<Module> modules;
    
    public internalModuleDependencyModule(List<Module> modules) {
        this.modules = modules;
    }

    @Override
    protected void configure() {
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                if (Module.class.isAssignableFrom(type.getRawType())) {
                    encounter.register(new InjectionListener<I>() {
                        @Override
                        public void afterInjection(final I injectee) {
                            LOG.info("Found module dependency : " + injectee.getClass().getCanonicalName());
                            modules.add((Module)injectee);
                        }
                    });
                }
            }
        });
    }
}
