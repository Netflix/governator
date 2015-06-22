package com.netflix.governator.guice;

import java.util.Collection;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.lifecycle.ClasspathScanner;

/**
 * Decorator for LifecycleInjectorBuilder which makes the original withModules methods 
 * additive instead of replacing any pre-defined module.  This class also acts as a default
 * implementation for overriding the behavior of LifecycleInjectorBuilder so that code
 * does not break every time a new method is added.
 * 
 * @author elandau
 */
public class DelegatingLifecycleInjectorBuilder implements LifecycleInjectorBuilder {
    private LifecycleInjectorBuilder delegate;
    
    public DelegatingLifecycleInjectorBuilder(LifecycleInjectorBuilder delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public LifecycleInjectorBuilder withBootstrapModule(BootstrapModule module) {
        this.delegate = delegate.withAdditionalBootstrapModules(module);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalBootstrapModules(
            BootstrapModule... modules) {
        this.delegate = delegate.withAdditionalBootstrapModules(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalBootstrapModules(
            Iterable<? extends BootstrapModule> modules) {
        this.delegate = delegate.withAdditionalBootstrapModules(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModules(Module... modules) {
        this.delegate = delegate.withAdditionalModules(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModules(
            Iterable<? extends Module> modules) {
        this.delegate = delegate.withAdditionalModules(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModules(
            Iterable<? extends Module> modules) {
        this.delegate = delegate.withAdditionalModules(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModules(Module... modules) {
        this.delegate = delegate.withAdditionalModules(modules);
        return this;
    }

    @Override
    @Deprecated
    public LifecycleInjectorBuilder withRootModule(Class<?> mainModule) {
        this.delegate = delegate.withRootModule(mainModule);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleClass(
            Class<? extends Module> module) {
        this.delegate = delegate.withAdditionalModuleClasses(module);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleClasses(
            Iterable<Class<? extends Module>> modules) {
        this.delegate = delegate.withAdditionalModuleClasses(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleClasses(
            Class<?>... modules) {
        this.delegate = delegate.withAdditionalModuleClasses(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModuleClasses(
            Iterable<Class<? extends Module>> modules) {
        this.delegate = delegate.withAdditionalModuleClasses(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModuleClasses(Class<?>... modules) {
        this.delegate = delegate.withAdditionalModuleClasses(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder ignoringAutoBindClasses(
            Collection<Class<?>> ignoreClasses) {
        this.delegate = delegate.ignoringAutoBindClasses(ignoreClasses);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder ignoringAllAutoBindClasses() {
        this.delegate = delegate.ignoringAllAutoBindClasses();
        return this;
    }

    @Override
    public LifecycleInjectorBuilder usingBasePackages(String... basePackages) {
        this.delegate = delegate.usingBasePackages(basePackages);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder usingBasePackages(
            Collection<String> basePackages) {
        this.delegate = delegate.usingBasePackages(basePackages);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder usingClasspathScanner(
            ClasspathScanner scanner) {
        this.delegate = delegate.usingClasspathScanner(scanner);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder inStage(Stage stage) {
        this.delegate = delegate.inStage(stage);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withMode(LifecycleInjectorMode mode) {
        this.delegate = delegate.withMode(mode);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleTransformer(
            ModuleTransformer transformer) {
        this.delegate = delegate.withModuleTransformer(transformer);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleTransformer(
            Collection<? extends ModuleTransformer> transformers) {
        this.delegate = delegate.withModuleTransformer(transformers);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleTransformer(
            ModuleTransformer... transformers) {
        this.delegate = delegate.withModuleTransformer(transformers);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withPostInjectorAction(
            PostInjectorAction action) {
        this.delegate = delegate.withPostInjectorAction(action);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withPostInjectorActions(
            Collection<? extends PostInjectorAction> actions) {
        this.delegate = delegate.withPostInjectorActions(actions);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withPostInjectorActions(
            PostInjectorAction... actions) {
        this.delegate = delegate.withPostInjectorActions(actions);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withoutModuleClasses(
            Iterable<Class<? extends Module>> modules) {
        this.delegate = delegate.withoutModuleClasses(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withoutModuleClasses(
            Class<? extends Module>... modules) {
        this.delegate = delegate.withoutModuleClasses(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withoutModuleClass(
            Class<? extends Module> module) {
        this.delegate = delegate.withoutModuleClass(module);
        return this;
    }
    
    @Override
    public LifecycleInjector build() {
        return delegate.build();
    }

    @Override
    @Deprecated
    public Injector createInjector() {
        return delegate.createInjector();
    }


}