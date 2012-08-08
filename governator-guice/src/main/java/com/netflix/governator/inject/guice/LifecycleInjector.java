package com.netflix.governator.inject.guice;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.annotations.RequiredAssets;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.AssetLoader;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;
import java.lang.annotation.Annotation;
import java.util.Arrays;
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
    private final LifecycleManager lifecycleManager;

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private List<Module> modules = Lists.newArrayList();
        private Collection<Class<?>> ignoreClasses = Lists.newArrayList();
        private Collection<String> basePackages = Lists.newArrayList("com", "org");
        private boolean ignoreAllClasses = false;
        private BootstrapModule bootstrapModule = null;

        public Builder withBootstrapModule(BootstrapModule module)
        {
            this.bootstrapModule = module;
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

        public Builder usingBasePackages(String... basePackages)
        {
            return usingBasePackages(Arrays.asList(basePackages));
        }

        public Builder usingBasePackages(Collection<String> basePackages)
        {
            this.basePackages = Lists.newArrayList(basePackages);
            return this;
        }

        public LifecycleInjector build()
        {
            return new LifecycleInjector(modules, ignoreClasses, ignoreAllClasses, bootstrapModule, basePackages);
        }

        public Injector createInjector()
        {
            return build().createInjector();
        }

        private Builder()
        {
        }
    }

    public LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    public Injector createChildInjector(Module... modules)
    {
        return createChildInjector(Arrays.asList(modules));
    }

    public Injector createChildInjector(Collection<Module> modules)
    {
        List<Module>            localModules = Lists.newArrayList(modules);
        localModules.add(new InternalLifecycleModule(lifecycleManager));
        return injector.createChildInjector(localModules);
    }

    public Injector createInjector()
    {
        List<Module>            localModules = Lists.newArrayList(modules);

        if ( !ignoreAllClasses )
        {
            Collection<Class<?>>    localIgnoreClasses = Lists.newArrayList(ignoreClasses);
            localIgnoreClasses.addAll(parentClasses);
            localIgnoreClasses.addAll(parentClasses);
            localModules.add(new InternalAutoBindModule(scanner, localIgnoreClasses));
        }

        return createChildInjector(localModules);
    }

    private LifecycleInjector(final List<Module> modules, Collection<Class<?>> ignoreClasses, boolean ignoreAllClasses, final BootstrapModule bootstrapModule, Collection<String> basePackages)
    {
        this.ignoreAllClasses = ignoreAllClasses;
        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
        this.modules = ImmutableList.copyOf(modules);

        List<Class<? extends Annotation>> annotations = Lists.newArrayList();
        annotations.add(AutoBindSingleton.class);
        annotations.add(RequiredAsset.class);
        annotations.add(RequiredAssets.class);
        scanner = new ClasspathScanner(basePackages, annotations);

        injector = Guice.createInjector
        (
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    if ( bootstrapModule != null )
                    {
                        bootstrapModule.configure(binder(), new RequiredAssetBinder(binder()));
                    }

                    bindLoaders(binder());
                    binder().bind(LifecycleManager.class).asEagerSingleton();
                }
            }
        );
        lifecycleManager = injector.getInstance(LifecycleManager.class);
    }

    private void bindLoaders(Binder binder)
    {
        for ( Class<?> clazz : scanner.get() )
        {
            if ( clazz.isAnnotationPresent(RequiredAsset.class) )
            {
                RequiredAsset       requiredAsset = clazz.getAnnotation(RequiredAsset.class);
                bindRequiredAsset(binder, requiredAsset);
            }
            else if ( clazz.isAnnotationPresent(RequiredAssets.class) )
            {
                RequiredAssets       requiredAssets = clazz.getAnnotation(RequiredAssets.class);
                for ( RequiredAsset requiredAsset : requiredAssets.value() )
                {
                    bindRequiredAsset(binder, requiredAsset);
                }
            }

            if ( clazz.isAnnotationPresent(AutoBindSingleton.class) )
            {
                if ( AssetLoader.class.isAssignableFrom(clazz) )
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends AssetLoader>    assetLoaderClass = (Class<? extends AssetLoader>)clazz;
                    RequiredAssetBinder.bindRequiredAsset(binder, LifecycleManager.DEFAULT_ASSET_LOADER_VALUE).to(assetLoaderClass);
                    parentClasses.add(clazz);
                }
                else if ( ConfigurationProvider.class.isAssignableFrom(clazz) )
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends ConfigurationProvider>    configurationProviderClass = (Class<? extends ConfigurationProvider>)clazz;
                    binder.bind(ConfigurationProvider.class).to(configurationProviderClass);
                    parentClasses.add(clazz);
                }
            }
        }
    }

    private void bindRequiredAsset(Binder binder, RequiredAsset requiredAsset)
    {
        if ( requiredAsset.loader() != AssetLoader.class )
        {
            RequiredAssetBinder.bindRequiredAsset(binder, requiredAsset.value()).to(requiredAsset.loader());
        }
    }
}
