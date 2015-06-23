package com.netflix.governator.guice.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorBuilderSuite;

@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Bootstrap {
	/**
	 * For most cases module() should suffice.  LifecycleInjectorBuilderSuite is being 
	 * deprecated in favor of plain guice Module's or BootstrapMoudle where absolutely 
	 * necessary.
	 * @deprecated 
	 */
    Class<? extends LifecycleInjectorBuilderSuite> value() default NullLifecycleInjectorBuilderSuite.class;
    Class<? extends BootstrapModule> bootstrap() default NullBootstrapModule.class;
    Class<? extends Module> module() default NullModule.class;
    
    public static class NullBootstrapModule implements BootstrapModule {
		@Override
		public void configure(BootstrapBinder binder) {
		}
    }
    
    public static class NullLifecycleInjectorBuilderSuite implements LifecycleInjectorBuilderSuite {
		@Override
		public void configure(LifecycleInjectorBuilder builder) {
		}
    }
    
    public static class NullModule implements Module {
		@Override
		public void configure(Binder binder) {
		}
    }
}
