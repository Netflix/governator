package com.netflix.governator.inject.guice;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.lifecycle.AssetLoader;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;
import java.lang.annotation.Annotation;
import java.util.List;

public class LifecycleManagerInjector
{
    private final ClasspathScanner scanner;
    private final Injector injector;

    public static LifecycleManagerInjector  get()
    {
        return get(null, new Module[0]);
    }

    public static LifecycleManagerInjector  get(ConfigurationProvider provider)
    {
        return get(provider, new Module[0]);
    }

    public static LifecycleManagerInjector  get(Module... modules)
    {
        return get(null, modules);
    }

    public static LifecycleManagerInjector  get(ConfigurationProvider provider, Module... modules)
    {
        return new LifecycleManagerInjector(provider, modules);
    }

    public ClasspathScanner getScanner()
    {
        return scanner;
    }

    public Injector getInjector()
    {
        return injector;
    }

    private LifecycleManagerInjector(final ConfigurationProvider provider, final Module... modules)
    {
        List<Class<? extends Annotation>> annotations = Lists.newArrayList();
        annotations.add(AutoBindSingleton.class);
        scanner = new ClasspathScanner(annotations);

        injector = Guice.createInjector
        (
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    if ( provider != null )
                    {
                        binder().bind(ConfigurationProvider.class).toInstance(provider);
                    }
                    for ( Module module : modules )
                    {
                        binder().install(module);
                    }

                    bindLoaders(binder());

                    binder().bind(LifecycleManager.class).asEagerSingleton();
                }
            }
        );
    }

    private void bindLoaders(Binder binder)
    {
        Multibinder<AssetLoader> multibinder = Multibinder.newSetBinder(binder, AssetLoader.class);

        List<Class<? extends Annotation>> annotations = Lists.newArrayList();
        annotations.add(AutoBindSingleton.class);
        ClasspathScanner scanner = new ClasspathScanner(annotations);
        for ( Class<?> clazz : scanner.get() )
        {
            if ( AssetLoader.class.isAssignableFrom(clazz) )
            {
                @SuppressWarnings("unchecked")
                Class<? extends AssetLoader>    assetLoaderClass = (Class<? extends AssetLoader>)clazz;
                multibinder.addBinding().to(assetLoaderClass);
            }
            else if ( ConfigurationProvider.class.isAssignableFrom(clazz) )
            {
                binder.bind(clazz).asEagerSingleton();
            }
        }
    }
}
