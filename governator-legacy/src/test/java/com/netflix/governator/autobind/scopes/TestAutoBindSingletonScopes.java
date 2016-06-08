package com.netflix.governator.autobind.scopes;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorMode;
import com.netflix.governator.guice.lazy.LazySingleton;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class TestAutoBindSingletonScopes {
	@AutoBindSingleton
	public static class AutoBindEagerSingleton {
		public static AtomicInteger counter = new AtomicInteger();
		public AutoBindEagerSingleton() {
			counter.incrementAndGet();
		}
		
	}
	
	@AutoBindSingleton(eager=false)
	public static class AutoBindNotEagerSingleton {
		public static AtomicInteger counter = new AtomicInteger();
		public AutoBindNotEagerSingleton() {
			counter.incrementAndGet();
		}
		
	}
	
	@AutoBindSingleton
	@LazySingleton
	public static class AutoBindLazySingleton {
		public static AtomicInteger counter = new AtomicInteger();
		public AutoBindLazySingleton() {
			counter.incrementAndGet();
		}
	}
	
	@Before
	public void before() {
        AutoBindEagerSingleton.counter.set(0);
        AutoBindLazySingleton.counter.set(0);
        AutoBindNotEagerSingleton.counter.set(0);
	}
	
    @Test
    public void scopesAreHonoredInDevMode() {
        Injector injector = LifecycleInjector.builder()
                .inStage(Stage.DEVELOPMENT)
                .usingBasePackages("com.netflix.governator.autobind.scopes")
                .build()
                .createInjector();
        
        injector.getInstance(AutoBindEagerSingleton.class);
        injector.getInstance(AutoBindEagerSingleton.class);
        
        Assert.assertEquals(1, AutoBindEagerSingleton.counter.get());
        Assert.assertEquals(0, AutoBindNotEagerSingleton.counter.get());
        Assert.assertEquals(0, AutoBindLazySingleton.counter.get());
    }
    
    @Test
    public void scopesAreHonoredInProd() {
        Injector injector = LifecycleInjector.builder()
                .inStage(Stage.PRODUCTION)
                .usingBasePackages("com.netflix.governator.autobind.scopes")
                .build()
                .createInjector();
        
        injector.getInstance(AutoBindEagerSingleton.class);
        injector.getInstance(AutoBindEagerSingleton.class);
        
        Assert.assertEquals(1, AutoBindEagerSingleton.counter.get());
        Assert.assertEquals(0, AutoBindNotEagerSingleton.counter.get());
        Assert.assertEquals(0, AutoBindLazySingleton.counter.get());
    }
    
    @Test
    public void scopesAreHonoredInDevModeNoChild() {
        Injector injector = LifecycleInjector.builder()
                .inStage(Stage.DEVELOPMENT)
                .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
                .usingBasePackages("com.netflix.governator.autobind.scopes")
                .build()
                .createInjector();
        
        injector.getInstance(AutoBindEagerSingleton.class);
        injector.getInstance(AutoBindEagerSingleton.class);
        
        Assert.assertEquals(1, AutoBindEagerSingleton.counter.get());
        Assert.assertEquals(0, AutoBindNotEagerSingleton.counter.get());
        Assert.assertEquals(0, AutoBindLazySingleton.counter.get());
    }
    
    @Test
    public void scopesAreHonoredInProdNoChild() {
        Injector injector = LifecycleInjector.builder()
                .inStage(Stage.PRODUCTION)
                .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
                .usingBasePackages("com.netflix.governator.autobind.scopes")
                .build()
                .createInjector();
        
        injector.getInstance(AutoBindEagerSingleton.class);
        injector.getInstance(AutoBindEagerSingleton.class);
        
        Assert.assertEquals(1, AutoBindEagerSingleton.counter.get());
        Assert.assertEquals(0, AutoBindNotEagerSingleton.counter.get());
        Assert.assertEquals(0, AutoBindLazySingleton.counter.get());
    }

}
