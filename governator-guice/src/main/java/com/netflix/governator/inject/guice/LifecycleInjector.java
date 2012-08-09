package com.netflix.governator.inject.guice;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.annotations.RequiredAssets;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LifecycleInjector
{
    private final ClasspathScanner scanner;
    private final List<Module> modules;
    private final Collection<Class<?>> ignoreClasses;
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
        return Guice.createInjector(localModules);
    }

    public Injector createInjector()
    {
        List<Module>            localModules = Lists.newArrayList(modules);

        if ( !ignoreAllClasses )
        {
            Collection<Class<?>>    localIgnoreClasses = Sets.newHashSet(ignoreClasses);
            localModules.add(new InternalAutoBindModule(scanner, localIgnoreClasses));
        }

        return createChildInjector(localModules);
    }

    private LifecycleInjector(final List<Module> modules, Collection<Class<?>> ignoreClasses, boolean ignoreAllClasses, BootstrapModule bootstrapModule, Collection<String> basePackages)
    {
        this.ignoreAllClasses = ignoreAllClasses;
        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
        this.modules = ImmutableList.copyOf(modules);

        List<Class<? extends Annotation>> annotations = Lists.newArrayList();
        annotations.add(AutoBindSingleton.class);
        annotations.add(RequiredAsset.class);
        annotations.add(RequiredAssets.class);
        scanner = new ClasspathScanner(basePackages, annotations);

        Injector        injector = Guice.createInjector(new InternalBootstrapModule(scanner, bootstrapModule));
        lifecycleManager = injector.getInstance(LifecycleManager.class);
    }
}
