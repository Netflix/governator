package com.netflix.governator.auto;

import com.google.inject.Module;

/**
 * To be use with any module added to the bootstrap phase of AutoModule and is used
 * to copy bindings from the bootstrap phase to the main injector.  
 * 
 * We choose to explicitly copy bindings rather than use child injectors due to 
 * various idiosyncrasies in child injectors (mostly surrounding singletons and optional
 * injection) as well as the complexity of copying all the bindings.
 * 
 * <pre>
 * {@code
 * public class SomeBootstrapModule extends AbstractModule {
 *    @Provides
 *    @Singleton
 *    @Named("MyExposedModule) // Must be named to avoid collisions
 *    public ModuleProvider getExposedModule(SomeBindingToExpose obj) {
 *        return new ModuleProvider() {
 *            @Override
 *            public Module get() {
 *                return new BootstrapExposedModule( ){
 *                    protected void configure() {
 *                        bind(SomeBindingToExpose.class).toInstance(obj);
 *                    }
 *                };
 *            }
 *        }
 *    }
 * }
 * }
 * </pre>
 * 
 * @author elandau
 */
public interface ModuleProvider {
    Module get();
}
