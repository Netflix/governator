package com.netflix.governator.auto;

import com.google.inject.AbstractModule;

/**
 * 'Hack' to expose bindings in a bootstrap module to the main injector without
 * having to copy bindings (doesn't work with multibindgs) or create child injectors.  
 * By a bootstrap module exposing a binding to a named BootstrapExposedModule the
 * bootstrap module may expose a curated set of bindings to be used by the main injector.
 * 
 * Note that we have to create the concept of BootstrapExposedModule because Guice 
 * blacklists bindings to Module and AbstractModule.
 * 
 * Example,
 * 
 * <pre>
 * {@code
 * public class SomeBootstrapModule extends AbstractModule {
 *    @Provides
 *    @Singleton
 *    @Named("MyExposedModule) 
 *    public BootstrapExposedModule getExposedModule(SomeBindingToExpose obj) {
 *        return new BootstrapExposedModule( ){
 *            protected void configure() {
 *                bind(SomeBindingToExpose.class).toInstance(obj);
 *            }
 *        };
 *    }
 * }
 * }
 * </pre>
 * 
 * @author elandau
 *
 */
public abstract class BootstrapExposedModule extends AbstractModule {

}
