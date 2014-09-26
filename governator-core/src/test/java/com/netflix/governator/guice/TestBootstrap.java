package com.netflix.governator.guice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOG = LoggerFactory.getLogger(TestBootstrap.class);
    
    public static class TestAction implements PostInjectorAction {
        private boolean injected = false;
        private final String name;
        public TestAction(String name) {
            this.name = name;
        }
        @Override
        public void call(Injector injector) {
            LOG.info("TestAction: " + name);
            injected = true;
        }
        String getName() {
            return name;
        }
        boolean isInjected() {
            return injected;
        }
    }
    
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
    @Bootstrap(Application2Bootstrap.class)
    public static @interface Application2 {
        String name();
    }

    public static class ApplicationBootstrap implements BootstrapModule {

        private Application application;
        
        @Inject
        public ApplicationBootstrap(Application application) {
            this.application = application;
        }

        @Override
        public void configure(BootstrapBinder binder) {
            binder.bindPostInjectorAction().toInstance(new TestAction(getClass().getSimpleName()));
            binder.bind(Application.class).toInstance(application);
            binder.include(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String.class).annotatedWith(Names.named("application")).toInstance(application.name());
                }
            });
        }
    }
    
    public static class Application2Bootstrap implements BootstrapModule {
        @Override
        public void configure(BootstrapBinder binder) {
            binder.bindPostInjectorAction().toInstance(new TestAction(getClass().getSimpleName()));
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
    @Application2(name="goo")
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
