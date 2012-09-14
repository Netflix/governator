package com.netflix.governator.guice;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class LifecycleInjectorBuilderImpl implements LifecycleInjectorBuilder
{
    private List<Module> modules = Lists.newArrayList();
    private Collection<Class<?>> ignoreClasses = Lists.newArrayList();
    private Collection<String> basePackages = Lists.newArrayList();
    private boolean ignoreAllClasses = false;
    private BootstrapModule bootstrapModule = null;
    private ClasspathScanner scanner = null;
    private LifecycleListener lifecycleListener = null;
    private Class<? extends LifecycleListener> lifecycleListenerClass = null;
    private Stage stage = Stage.PRODUCTION;

    @Override
    public LifecycleInjectorBuilder withBootstrapModule(BootstrapModule module)
    {
        this.bootstrapModule = module;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModules(Module... modules)
    {
        this.modules = ImmutableList.copyOf(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModules(Iterable<? extends Module> modules)
    {
        this.modules = ImmutableList.copyOf(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder ignoringAutoBindClasses(Collection<Class<?>> ignoreClasses)
    {
        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder ignoringAllAutoBindClasses()
    {
        this.ignoreAllClasses = true;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder usingBasePackages(String... basePackages)
    {
        return usingBasePackages(Arrays.asList(basePackages));
    }

    @Override
    public LifecycleInjectorBuilder usingBasePackages(Collection<String> basePackages)
    {
        this.basePackages = Lists.newArrayList(basePackages);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder usingClasspathScanner(ClasspathScanner scanner)
    {
        this.scanner = scanner;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withLifecycleListener(LifecycleListener lifecycleListener)
    {
        this.lifecycleListener = lifecycleListener;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withLifecycleListener(Class<? extends LifecycleListener> lifecycleListener)
    {
        lifecycleListenerClass = lifecycleListener;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder inStage(Stage stage)
    {
        this.stage = stage;
        return this;
    }

    @Override
    public LifecycleInjector build()
    {
        return new LifecycleInjector(modules, ignoreClasses, ignoreAllClasses, bootstrapModule, scanner, basePackages, lifecycleListener, lifecycleListenerClass, stage);
    }

    @Override
    public Injector createInjector()
    {
        return build().createInjector();
    }

    LifecycleInjectorBuilderImpl()
    {
    }
}
