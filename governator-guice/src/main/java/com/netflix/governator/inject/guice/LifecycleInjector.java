package com.netflix.governator.inject.guice;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.SystemConfigurationProvider;
import com.netflix.governator.lifecycle.AssetLoader;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

public class LifecycleInjector
{
    private final ClasspathScanner scanner;
    private final Injector injector;
    private final List<Module> modules;
    private final Collection<Class<?>> ignoreClasses;
    private final Collection<Class<?>> parentClasses = Lists.newArrayList();
    private final boolean ignoreAllClasses;

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private ConfigurationProvider provider;
        private List<Module> modules = Lists.newArrayList();
        private Collection<Class<?>> ignoreClasses = Lists.newArrayList();
        private boolean ignoreAllClasses = false;

        public Builder usingConfigurationProvider(ConfigurationProvider provider)
        {
            this.provider = provider;
            return this;
        }

        public Builder withModules(Module... modules)
        {
            this.modules = ImmutableList.copyOf(modules);
            return this;
        }

        public Builder withModules(Iterable<? extends Module> modules)
        {
            this.modules = ImmutableList.copyOf(modules);
            return this;
        }

        public Builder ignoringAutoBindClasses(Collection<Class<?>> ignoreClasses)
        {
            this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
            return this;
        }

        public Builder ignoringAllAutoBindClasses()
        {
            this.ignoreAllClasses = true;
            return this;
        }

        public LifecycleInjector build()
        {
            ConfigurationProvider localProvider = (provider != null) ? provider : new SystemConfigurationProvider();
            return new LifecycleInjector(localProvider, modules, ignoreClasses, ignoreAllClasses);
        }

        public Injector createInjector()
        {
            return build().createInjector();
        }

        private Builder()
        {
        }
    }

    public Injector createInjector()
    {
        List<Module>            localModules = Lists.newArrayList(modules);

        if ( !ignoreAllClasses )
        {
            Collection<Class<?>>    localIgnoreClasses = Lists.newArrayList(ignoreClasses);
            localIgnoreClasses.addAll(parentClasses);
            localIgnoreClasses.addAll(parentClasses);
            localModules.add(new GuiceAutoBindModule(scanner, localIgnoreClasses));
        }

        localModules.add(new LifecycleModule(injector.getInstance(LifecycleManager.class)));
        return injector.createChildInjector(localModules);
    }

    private LifecycleInjector(final ConfigurationProvider provider, final List<Module> modules, Collection<Class<?>> ignoreClasses, boolean ignoreAllClasses)
    {
        this.ignoreAllClasses = ignoreAllClasses;
        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
        List<Class<? extends Annotation>> annotations = Lists.newArrayList();
        annotations.add(AutoBindSingleton.class);
        scanner = new ClasspathScanner(annotations);
        this.modules = ImmutableList.copyOf(modules);

        injector = Guice.createInjector
        (
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    binder().bind(ConfigurationProvider.class).toInstance(provider);
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
                parentClasses.add(clazz);
            }
            else if ( ConfigurationProvider.class.isAssignableFrom(clazz) )
            {
                binder.bind(clazz).asEagerSingleton();
                parentClasses.add(clazz);
            }
        }
    }
}
