/**
 * This test is in an 'external' module to bypass the restriction that governator
 * internal modules cannot be picked up via module depdency
 */

package com.netflix.external.governator.guice.modules;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ModuleDepdenciesTest extends LifecycleInjectorBuilderProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(ModuleDepdenciesTest.class);

    private static final TypeLiteral<List<Integer>> LIST_TYPE_LITERAL = new TypeLiteral<List<Integer>>() { };
    
    private static AtomicLong counter = new AtomicLong(0);

    @Singleton
    public static class ModuleA extends AbstractModule
    {
        public ModuleA()
        {
            LOG.info("ModuleA created");
        }

        @Override
        protected void configure()
        {
            LOG.info("ConfigureA");
            counter.incrementAndGet();
        }
    }

    @AfterMethod
    public void afterEachTest()
    {
        counter.set(0);
    }

    @Singleton
    public static class ModuleB extends AbstractModule
    {
        @Inject
        public ModuleB(ModuleA a)
        {
            LOG.info("ModuleB created");
        }

        @Override
        protected void configure()
        {
            LOG.info("ConfigureB");
            counter.incrementAndGet();
        }
    }

    @Test(dataProvider = "builders")
    public void testModuleDependency(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        lifecycleInjectorBuilder.withAdditionalModuleClasses(ModuleB.class).build().createInjector();
    }

    @Singleton
    public static class A1
    {
        @Inject
        public A1(List<Integer> list)
        {
            list.add(1);
        }
    }

    @Singleton
    public static class A2
    {
        @Inject
        public A2(List<Integer> list)
        {
            list.add(2);
        }
    }

    @Singleton
    public static class A3
    {
        @Inject
        public A3(List<Integer> list)
        {
            list.add(3);
        }
    }

    @Singleton
    public static class A4
    {
        @Inject
        public A4(List<Integer> list)
        {
            list.add(4);
        }
    }

    @Singleton
    public static class ModuleA1 extends AbstractModule
    {
        protected void configure()
        {
            bind(A1.class);
        }
    }

    @Singleton
    public static class ModuleA2 extends AbstractModule
    {
        @Inject
        public ModuleA2(ModuleA1 moduleA1)
        {
        }

        public ModuleA2()
        {
        }

        protected void configure()
        {
            bind(A2.class);
        }
    }

    @Singleton
    public static class ModuleA3 extends AbstractModule
    {
        public ModuleA3()
        {
        }

        @Inject
        public ModuleA3(ModuleA2 moduleA3)
        {
        }

        protected void configure()
        {
            bind(A3.class);
        }
    }

    @Singleton
    public static class ModuleA4 extends AbstractModule
    {
        public ModuleA4()
        {
        }

        @Inject
        public ModuleA4(ModuleA3 moduleA3)
        {
        }

        protected void configure()
        {
            bind(A4.class);
        }
    }

    @Test(dataProvider = "builders")
    public void confirmBindingSingletonOrder(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        final List<Integer> actual = Lists.newArrayList();
        final List<Module> modules = Lists.<Module>newArrayList(new ModuleA1(), new ModuleA2(), new ModuleA3(), new ModuleA4());
        BootstrapModule bootstrap = new BootstrapModule()
        {
            @Override
            public void configure(BootstrapBinder binder)
            {
                binder.bind(LIST_TYPE_LITERAL).toInstance(actual);
            }
        };

        // Confirm that singletons are created in binding order
        final List<Integer> expected = Lists.newArrayList(1, 2, 3, 4);
        Injector injector = lifecycleInjectorBuilder.inStage(Stage.PRODUCTION).withModules(modules).withBootstrapModule(bootstrap).build().createInjector();
        Assert.assertEquals(actual, expected);
    }

    @Test(dataProvider = "builders")
    public void confirmBindingReverseSingletonOrder(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        final List<Integer> actual = Lists.newArrayList();
        final List<Module> modules = Lists.<Module>newArrayList(new ModuleA1(), new ModuleA2(), new ModuleA3(), new ModuleA4());
        BootstrapModule bootstrap = new BootstrapModule()
        {
            @Override
            public void configure(BootstrapBinder binder)
            {
                binder.bind(LIST_TYPE_LITERAL).toInstance(actual);
            }
        };

        // Reverse the order of modules in the list to confirm that singletons
        // are now created in reverse order
        final List<Integer> expected = Lists.newArrayList(1, 2, 3, 4);
        Injector injector = lifecycleInjectorBuilder.inStage(Stage.PRODUCTION).withModules(Lists.reverse(modules)).withBootstrapModule(bootstrap).build().createInjector();
        LOG.info(actual.toString());
        Assert.assertEquals(actual, Lists.reverse(expected));
    }
    
    @Test(dataProvider = "builders") 
    public void confirmMultiWithModuleClasses(LifecycleInjectorBuilder lifecycleInjectorBuilder) 
    {
        final List<Integer> actual = Lists.newArrayList();
        BootstrapModule bootstrap = new BootstrapModule()
        {
            @Override
            public void configure(BootstrapBinder binder)
            {
                binder.bind(LIST_TYPE_LITERAL).toInstance(actual);
            }
        };

        Injector injector = lifecycleInjectorBuilder
                .inStage(Stage.PRODUCTION)
                .withModuleClasses(ModuleA2.class, ModuleA3.class)
                .withBootstrapModule(bootstrap).build().createInjector();
        
        final List<Integer> expected = Lists.newArrayList(1, 2, 3);
        LOG.info(actual.toString());
        Assert.assertEquals(actual, expected);
    }
}
