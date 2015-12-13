package com.netflix.governator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementVisitor;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;
import com.netflix.governator.spi.InjectorCreator;
import com.netflix.governator.spi.ModuleTransformer;
import com.netflix.governator.visitors.IsStaticInjectionVisitor;
import com.netflix.governator.visitors.KeyTracingVisitor;
import com.netflix.governator.visitors.WarnOfStaticInjectionVisitor;

/**
 * Simple DSL on top of Guice through which a top level sequence of operations
 * and transformations of Guice modules and bindings may be performed.  Operations
 * are tracked using a single module and are additive such that each operation 
 * executes on top of the entire current binding state.  Once all bindings have 
 * been defined the injector may created using an {@link InjectorCreator} strategy.
 * 
 * <code>
 * ModuleBuilder
 *      .fromModule(new MyApplicationModule())
 *      .overrideWith(new OverridesForTesting())
 *      .forEachElement(new BindingTracingVisitor())
 *      .createInjector();
 * </code>
 */
public final class ModuleBuilder implements Module {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleBuilder.class);
    
    private static final Stage LAZY_SINGLETONS_STAGE = Stage.DEVELOPMENT;
    
    private Module module;
    
    /**
     * Start the builder using the specified module. 
     * 
     * @param module
     * @return
     */
    public static ModuleBuilder fromModule(Module module) {
        return new ModuleBuilder(module);
    }
    
    public static ModuleBuilder fromModules(Module firstModule, Module secondModule, Module ... additionalModules) {
        List<Module> modules = new ArrayList<>();
        modules.add(firstModule);
        modules.add(secondModule);
        if (null != additionalModules) {
            modules.addAll(Arrays.asList(additionalModules));
        }
        return new ModuleBuilder(Modules.combine(modules));
    }
    
    public static ModuleBuilder fromModules(List<Module> modules) {
        return new ModuleBuilder(Modules.combine(modules));
    }
    
    public static ModuleBuilder createDefault() {
        return fromModule(new Module() {
            @Override
            public void configure(Binder binder) {
            }
        });
    }
    
    private ModuleBuilder(Module module) {
        this.module = module;
    }
    
    /**
     * Override all existing bindings with bindings in the provided modules.
     * This method uses Guice's build in {@link Modules#override} and is preferable
     * to using {@link Modules#override}.  The approach here is to attempt to promote 
     * the use of {@link Modules#override} as a single top level override.  Using
     * {@link Modules#override} inside Guice modules can result in duplicate bindings 
     * when the same module is installed in multiple placed. 
     * @param modules
     */
    public ModuleBuilder overrideWith(Module ... modules) {
        return overrideWith(Arrays.asList(modules));
    }
    
    /**
     * @see ModuleBuilder#overrideWith(Module...)
     */
    public ModuleBuilder overrideWith(Collection<Module> modules) {
        this.module = Modules.override(module).with(modules);
        return this;
    }
    
    /**
     * Add additional bindings to the module tracked by the DSL
     * @param modules
     */
    public ModuleBuilder combineWith(Module ... modules) {
        List<Module> m = new ArrayList<>();
        m.add(module);
        m.addAll(Arrays.asList(modules));
        this.module = Modules.combine(m);
        return this;
    }
    
    /**
     * Iterator through all elements and invoke a visitor.  This method
     * is mean for side-effect free operations such as logging the current 
     * state of bindings.
     * @param visitor
     */
    public ModuleBuilder forEachElement(ElementVisitor<Void> visitor) {
        for (Element element : Elements.getElements(module)) {
            element.acceptVisitor(visitor);
        }
        return this;
    }
    
    /**
     * Log a warning that static injection is being used.  Static injection is considered a 'hack'
     * to alllow for backwards compatibility with non DI'd static code.
     */
    public ModuleBuilder warnOfStaticInjections() {
        return forEachElement(new WarnOfStaticInjectionVisitor());
    }

    /**
     * Log the current binding state.  Log() is useful for debugging a sequence of
     * operation where the binding snapshot can be dumped to the log after an operation.
     * 
     * @param prefix String prefix appended to each log message
     */
    public ModuleBuilder logEachKey(final String prefix) {
        return forEachElement(new KeyTracingVisitor(prefix));
    }
    
    /**
     * @see {@link ModuleBuilder#logEachKey(String prefix)}
     */
    public ModuleBuilder logEachKey() {
        return logEachKey("");
    }
    
    /**
     * Log a single 'info' line.  Call info in conjunction with calls to other logging calls
     * to include headers and footers in the log.
     * @param message
     * @return
     */
    public ModuleBuilder info(String message) {
        LOG.info(message);
        return this;
    }
    
    /**
     * Extend the CORE dsl by providing a custom ModuleTransformer
     * @param transformer
     */
    public ModuleBuilder transform(ModuleTransformer transformer) {
        this.module = transformer.transform(module);
        return this;
    }
    
    /**
     * Filter out elements for which the provided visitor returns true
     * @param visitor
     */
    public ModuleBuilder filter(ElementVisitor<Boolean> visitor) {
        List<Element> elements = new ArrayList<Element>();
        for (Element element : Elements.getElements(module)) {
            if (!element.acceptVisitor(visitor)) {
                elements.add(element);
            }
        }
        this.module = Elements.getModule(elements);
        return this;
    }
    
    /**
     * Filter out all bindings using requestStaticInjection
     * @return
     */
    public ModuleBuilder stripStaticInjections() {
        return filter(new IsStaticInjectionVisitor());
    }
    
    /**
     * Create the injector in the specified stage using the specified InjectorCreator
     * strategy.  The InjectorCreator will most likely perform additional error handling on top 
     * of the call to {@link Guice#createInjector}.
     * 
     * @param stage     Stage in which the injector is running.  It is recommended to run in Stage.DEVELOPEMENT
     *                  since it treats all singletons as lazy as opposed to defaulting to eager instantiation which 
     *                  could result in instantiating unwanted classes.
     * @param creator   
     */
    public <I extends Injector> I createInjector(Stage stage, InjectorCreator<I> creator) {
        return creator.createInjector(stage, this);
    }
    
    /**
     * @see {@link ModuleBuilder#createInjector(Stage, InjectorCreator)}
     */
    public <I extends Injector> I createInjector(InjectorCreator<I> creator) {
        return creator.createInjector(LAZY_SINGLETONS_STAGE, this);
    }
    
    /**
     * @see {@link ModuleBuilder#createInjector(Stage, InjectorCreator)}
     */
    public Injector createInjector(Stage stage) {
        return createInjector(stage, new LifecycleInjectorCreator());
    }

    /**
     * @see {@link ModuleBuilder#createInjector(Stage, InjectorCreator)}
     */
    public Injector createInjector() {
        return createInjector(LAZY_SINGLETONS_STAGE, new LifecycleInjectorCreator());
    }

    @Override
    public void configure(Binder binder) {
        binder.skipSources(getClass());
        binder.install(module);
    }
    
}
