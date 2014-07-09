package com.netflix.governator.guice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.annotations.Bootstrap;

public class TestBootstrap {
    
    @Documented
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Bootstrap(ApplicationBootstrap.class)
    public static @interface Application {
        String name();
    }

    @Documented
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Bootstrap(ApplicationBootstrap.class)
    public static @interface Multiple {
        String name();
    }

    public static class ApplicationBootstrap implements LifecycleInjectorBuilderSuite {

        private Application application;
        
        @Inject
        public ApplicationBootstrap(Application application) {
            this.application = application;
        }
        
        @Override
        public void configure(LifecycleInjectorBuilder builder) {
            builder.withModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String.class).annotatedWith(Names.named("application")).toInstance(application.name());
                }
            });
        }
    }
    
    public static class InitModule extends AbstractModule {
        @Inject
        InitModule(Application application) {
            Assert.assertEquals("foo", application.name());
        }
        
        @Override
        protected void configure() {
            bind(AtomicInteger.class).annotatedWith(Names.named("init")).toInstance(new AtomicInteger());
        }
    }

    @Application(name="foo")
    @Modules(include={InitModule.class})
    public static class MyApplication extends AbstractModule {
        @Override
        protected void configure() {
            bind(String.class).annotatedWith(Names.named("bar")).toInstance("test");
        }
    }
    
    @Test
    public void testAnnotationWiringAndInjection() {
        Injector injector = LifecycleInjector.bootstrap(MyApplication.class);
        String appName = injector.getInstance(Key.get(String.class, Names.named("application")));
        Assert.assertEquals("foo", appName);
        Assert.assertEquals("test", injector.getInstance(Key.get(String.class, Names.named("bar"))));
    }

}
