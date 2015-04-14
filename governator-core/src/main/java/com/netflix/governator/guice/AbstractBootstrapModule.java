package com.netflix.governator.guice;

import java.util.Collection;

import com.google.common.collect.Lists;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.Message;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.ResourceLocator;

public abstract class AbstractBootstrapModule implements BootstrapModule {

    private BootstrapBinder binder;
    
    @Override
    final public void configure(BootstrapBinder binder) {
        assert this.binder == null;
        
        this.binder = binder;
        try {
            configure();
        }
        finally {
            this.binder = null;
        }
    }
    
    protected abstract void configure();
    
    /**
     * Bind actions to perform after the injector is created.
     * 
     * @return a binding builder used to add a new element in the set.
     */
    public LinkedBindingBuilder<PostInjectorAction> bindPostInjectorAction() {
        return Multibinder.newSetBinder(binder, PostInjectorAction.class).addBinding();
    }

    /**
     * Bind module transform operations to perform on the final list of modules.
     * 
     * @return a binding builder used to add a new element in the set.
     */
    public LinkedBindingBuilder<ModuleTransformer> bindModuleTransformer() {
        return Multibinder.newSetBinder(binder, ModuleTransformer.class).addBinding();
    }

    /**
     * Use this to bind a {@link LifecycleListener}. It internally uses a Multibinder to do the
     * binding so that you can bind multiple LifecycleListeners
     *
     * @return a binding builder used to add a new element in the set.
     */
    public LinkedBindingBuilder<LifecycleListener> bindLifecycleListener() {
        return Multibinder.newSetBinder(binder, LifecycleListener.class).addBinding();
    }

    /**
     * Use this to bind a {@link ResourceLocator}. It internally uses a Multibinder to do the
     * binding so that you can bind multiple ResourceLocators
     *
     * @return a binding builder used to add a new element in the set.
     */
    public LinkedBindingBuilder<ResourceLocator> bindResourceLocator() {
        return Multibinder.newSetBinder(binder, ResourceLocator.class).addBinding();
    }

    public void include(Class<? extends Module> module) {
        this.binder.include(module);
    }
    
    public void include(Class<? extends Module> ... modules) {
        this.binder.include(Lists.newArrayList(modules));
    }
    
    public void include(Collection<Class<? extends Module>> modules) {
        binder.include(modules);
    }
    
    public void include(Module module) {
        binder.include(module);
    }

    public void includeModules(Collection<? extends Module> modules) {
        binder.includeModules(modules);
    }

    public void includeModules(Module ... modules) {
        binder.includeModules(Lists.newArrayList(modules));
    }

    public void exclude(Class<? extends Module> module) {
        binder.exclude(module);
    }
    
    public void exclude(Class<? extends Module> ... modules) {
        binder.exclude(Lists.newArrayList(modules));
    }
    
    public void exclude(Collection<Class<? extends Module>> modules) {
        binder.exclude(modules);
    }
    
    public void addError(String message, Object... arguments) {
        binder.addError(message, arguments);
    }

    public void addError(Throwable t) {
        binder.addError(t);
    }

    public void addError(Message message) {
        binder.addError(message);
    }

    public <T> Provider<T> getProvider(Key<T> key) {
        return binder.getProvider(key);
    }

    public <T> Provider<T> getProvider(Class<T> type) {
        return binder.getProvider(type);
    }

    public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral) {
        return binder.getMembersInjector(typeLiteral);
    }

    public <T> MembersInjector<T> getMembersInjector(Class<T> type) {
        return binder.getMembersInjector(type);
    }

    public LinkedBindingBuilder<ConfigurationProvider> bindConfigurationProvider() {
        return binder.bindConfigurationProvider();
    }

    public void disableAutoBinding() {
        binder.disableAutoBinding();
    }
    
    public void inStage(Stage stage) {
        binder.inStage(stage);
    }

    public void inMode(LifecycleInjectorMode mode) {
        binder.inMode(mode);
    }
    
    protected BootstrapBinder binder() {
        return binder;
    }

}
