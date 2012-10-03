package com.netflix.governator.autobind;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.guice.AutoBindProvider;
import com.netflix.governator.guice.AutoBinds;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

public class TestAutoBind
{
    @Test
    public void     testSimple()
    {
        final AutoBindProvider<AutoBind> provider = new AutoBindProvider<AutoBind>()
        {
            @Override
            public void configureForConstructor(Binder binder, AutoBind autoBindAnnotation, Constructor constructor, int argumentIndex)
            {
                binder.bind(String.class).annotatedWith(autoBindAnnotation).toInstance("a is a");
            }

            @Override
            public void configureForMethod(Binder binder, AutoBind autoBindAnnotation, Method method, int argumentIndex)
            {
            }

            @Override
            public void configureForField(Binder binder, AutoBind autoBindAnnotation, Field field)
            {
            }
        };

        Injector injector = LifecycleInjector
            .builder()
            .ignoringAutoBindClasses(Collections.<Class<?>>singleton(ObjectWithCustomAutoBind.class))
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        binder.bind(new TypeLiteral<AutoBindProvider<AutoBind>>(){}).toInstance(provider);
                    }
                }
            )
            .usingBasePackages("com.netflix.governator.autobind")
            .createInjector();
        SimpleAutoBind instance = injector.getInstance(SimpleAutoBind.class);
        Assert.assertEquals(instance.getString(), "a is a");
    }

    @Test
    public void     testCustom()
    {
        final AutoBindProvider<CustomAutoBind> provider = new CustomAutoProvider();

        @SuppressWarnings("RedundantCast")
        Injector injector = LifecycleInjector
            .builder()
            .ignoringAutoBindClasses(Lists.newArrayList((Class<?>)SimpleAutoBind.class, (Class<?>)SimpleWithMultipleAutoBinds.class, (Class<?>)SimpleWithFieldAutoBind.class, (Class<?>)SimpleWithMethodAutoBind.class))
            .withBootstrapModule
                (
                    new BootstrapModule()
                    {
                        @Override
                        public void configure(BootstrapBinder binder)
                        {
                            binder.bind(new TypeLiteral<AutoBindProvider<CustomAutoBind>>(){}).toInstance(provider);
                        }
                    }
                )
            .usingBasePackages("com.netflix.governator.autobind")
            .createInjector();
        ObjectWithCustomAutoBind instance = injector.getInstance(ObjectWithCustomAutoBind.class);
        Assert.assertEquals(instance.getInjectable().getStr(), "hey");
        Assert.assertEquals(instance.getInjectable().getValue(), 1234);
    }

    @Test
    public void     testMultiple()
    {
        final AutoBindProvider<AutoBind> provider = new AutoBindProvider<AutoBind>()
        {
            @Override
            public void configureForConstructor(Binder binder, AutoBind autoBindAnnotation, Constructor constructor, int argumentIndex)
            {
                binder.bind(MockWithParameter.class).annotatedWith(autoBindAnnotation).toInstance(new MockWithParameter(autoBindAnnotation.value()));
            }

            @Override
            public void configureForMethod(Binder binder, AutoBind autoBindAnnotation, Method method, int argumentIndex)
            {
            }

            @Override
            public void configureForField(Binder binder, AutoBind autoBindAnnotation, Field field)
            {
            }
        };

        Injector injector = LifecycleInjector
            .builder()
            .ignoringAutoBindClasses(Collections.<Class<?>>singleton(ObjectWithCustomAutoBind.class))
            .withBootstrapModule
                (
                    new BootstrapModule()
                    {
                        @Override
                        public void configure(BootstrapBinder binder)
                        {
                            binder.bind(new TypeLiteral<AutoBindProvider<AutoBind>>(){}).toInstance(provider);
                        }
                    }
                )
            .usingBasePackages("com.netflix.governator.autobind")
            .createInjector();
        SimpleWithMultipleAutoBinds instance = injector.getInstance(SimpleWithMultipleAutoBinds.class);
        Assert.assertEquals(instance.getArg1().getParameter(), "one");
        Assert.assertEquals(instance.getArg2().getParameter(), "two");
        Assert.assertEquals(instance.getArg3().getParameter(), "three");
        Assert.assertEquals(instance.getArg4().getParameter(), "four");
    }

    @Test
    public void     testField()
    {
        final AutoBindProvider<AutoBind> provider = new AutoBindProvider<AutoBind>()
        {
            @Override
            public void configureForConstructor(Binder binder, AutoBind autoBindAnnotation, Constructor constructor, int argumentIndex)
            {
            }

            @Override
            public void configureForMethod(Binder binder, AutoBind autoBindAnnotation, Method method, int argumentIndex)
            {
            }

            @Override
            public void configureForField(Binder binder, AutoBind autoBindAnnotation, Field field)
            {
                binder.bind(MockWithParameter.class).annotatedWith(autoBindAnnotation).toInstance(new MockWithParameter(autoBindAnnotation.value()));
            }
        };

        Injector injector = LifecycleInjector
            .builder()
            .ignoringAutoBindClasses(Collections.<Class<?>>singleton(ObjectWithCustomAutoBind.class))
            .withBootstrapModule
                (
                    new BootstrapModule()
                    {
                        @Override
                        public void configure(BootstrapBinder binder)
                        {
                            binder.bind(new TypeLiteral<AutoBindProvider<AutoBind>>()
                            {
                            }).toInstance(provider);
                        }
                    }
                )
            .usingBasePackages("com.netflix.governator.autobind")
            .createInjector();
        SimpleWithFieldAutoBind instance = injector.getInstance(SimpleWithFieldAutoBind.class);
        Assert.assertEquals(instance.f1.getParameter(), "f1");
        Assert.assertEquals(instance.f2.getParameter(), "f2");
    }

    @Test
    public void     testMethod()
    {
        final AutoBindProvider<AutoBind> provider = new AutoBindProvider<AutoBind>()
        {
            @Override
            public void configureForConstructor(Binder binder, AutoBind autoBindAnnotation, Constructor constructor, int argumentIndex)
            {
            }

            @Override
            public void configureForMethod(Binder binder, AutoBind autoBindAnnotation, Method method, int argumentIndex)
            {
                binder.bind(MockWithParameter.class).annotatedWith(autoBindAnnotation).toInstance(new MockWithParameter(autoBindAnnotation.value()));
            }

            @Override
            public void configureForField(Binder binder, AutoBind autoBindAnnotation, Field field)
            {
            }
        };

        Injector injector = LifecycleInjector
            .builder()
            .ignoringAutoBindClasses(Collections.<Class<?>>singleton(ObjectWithCustomAutoBind.class))
            .withBootstrapModule
                (
                    new BootstrapModule()
                    {
                        @Override
                        public void configure(BootstrapBinder binder)
                        {
                            binder.bind(new TypeLiteral<AutoBindProvider<AutoBind>>(){}).toInstance(provider);
                        }
                    }
                )
            .usingBasePackages("com.netflix.governator.autobind")
            .createInjector();
        SimpleWithMethodAutoBind instance = injector.getInstance(SimpleWithMethodAutoBind.class);
        Assert.assertEquals(instance.getF1().getParameter(), "f1");
        Assert.assertEquals(instance.getF2().getParameter(), "f2");
    }

    @Test
    public void     testNormally()
    {
        Injector        injector = Guice.createInjector
        (
            new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    binder.bind(String.class).annotatedWith(AutoBinds.withValue("foo")).toInstance("we are the music makers");
                }
            }
        );

        Assert.assertEquals(injector.getInstance(SimpleAutoBind.class).getString(), "we are the music makers");
    }
}
