/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.autobind;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.guice.AutoBindProvider;
import com.netflix.governator.guice.AutoBinds;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Collections;

public class TestAutoBind extends LifecycleInjectorBuilderProvider
{
    @Test(dataProvider = "builders")
    public void     testSimple(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        final AutoBindProvider<AutoBind> provider = new AutoBindProvider<AutoBind>()
        {
            @Override
            public void configure(Binder binder, AutoBind autoBindAnnotation)
            {
                binder.bind(String.class).annotatedWith(autoBindAnnotation).toInstance("a is a");
            }
        };

        Injector injector = lifecycleInjectorBuilder
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

    @Test(dataProvider = "builders")
    public void     testCustom(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        @SuppressWarnings("RedundantCast")
        Injector injector = lifecycleInjectorBuilder
            .ignoringAutoBindClasses(Lists.newArrayList((Class<?>)SimpleAutoBind.class, (Class<?>)SimpleWithMultipleAutoBinds.class, (Class<?>)SimpleWithFieldAutoBind.class, (Class<?>)SimpleWithMethodAutoBind.class))
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        binder.bind(new TypeLiteral<AutoBindProvider<CustomAutoBind>>(){}).to(CustomAutoBindProvider.class).asEagerSingleton();
                    }
                }
            )
            .usingBasePackages("com.netflix.governator.autobind")
            .createInjector();
        ObjectWithCustomAutoBind instance = injector.getInstance(ObjectWithCustomAutoBind.class);
        Assert.assertEquals(instance.getInjectable().getStr(), "hey");
        Assert.assertEquals(instance.getInjectable().getValue(), 1234);
    }

    @Test(dataProvider = "builders")
    public void     testMultiple(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        final AutoBindProvider<AutoBind> provider = new AutoBindProvider<AutoBind>()
        {
            @Override
            public void configure(Binder binder, AutoBind autoBindAnnotation)
            {
                binder.bind(MockWithParameter.class).annotatedWith(autoBindAnnotation).toInstance(new MockWithParameter(autoBindAnnotation.value()));
            }
        };

        Injector injector = lifecycleInjectorBuilder
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

    @Test(dataProvider = "builders")
    public void     testField(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        final AutoBindProvider<AutoBind> provider = new AutoBindProvider<AutoBind>()
        {
            @Override
            public void configure(Binder binder, AutoBind autoBindAnnotation)
            {
                binder.bind(MockWithParameter.class).annotatedWith(autoBindAnnotation).toInstance(new MockWithParameter(autoBindAnnotation.value()));
            }
        };

        Injector injector = lifecycleInjectorBuilder
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
        Assert.assertEquals(instance.field1.getParameter(), "field1");
        Assert.assertEquals(instance.field2.getParameter(), "field2");
    }

    @Test(dataProvider = "builders")
    public void     testMethod(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        final AutoBindProvider<AutoBind> provider = new AutoBindProvider<AutoBind>()
        {
            @Override
            public void configure(Binder binder, AutoBind autoBindAnnotation)
            {
                binder.bind(MockWithParameter.class).annotatedWith(autoBindAnnotation).toInstance(new MockWithParameter(autoBindAnnotation.value()));
            }
        };

        Injector injector = lifecycleInjectorBuilder
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

    private static class CustomAutoBindProvider implements AutoBindProvider<CustomAutoBind>
    {
        @Override
        public void configure(Binder binder, CustomAutoBind custom)
        {
            binder.bind(MockInjectable.class).annotatedWith(custom).toInstance(new MockInjectable(custom.str(), custom.value()));
        }
    }
}
