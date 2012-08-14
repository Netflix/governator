package com.netflix.governator.inject.guice;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleListener;
import java.util.Collection;

public interface Builder
{
    public Builder withBootstrapModule(BootstrapModule module);

    public Builder withModules(Module... modules);

    public Builder withModules(Iterable<? extends Module> modules);

    public Builder ignoringAutoBindClasses(Collection<Class<?>> ignoreClasses);

    public Builder ignoringAllAutoBindClasses();

    public Builder usingBasePackages(String... basePackages);

    public Builder usingBasePackages(Collection<String> basePackages);

    public Builder usingClasspathScanner(ClasspathScanner scanner);

    public Builder withLifecycleListener(LifecycleListener lifecycleListener);

    public LifecycleInjector build();

    public Injector createInjector();
}
