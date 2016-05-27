package com.netflix.governator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementVisitor;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;
import com.netflix.governator.spi.InjectorCreator;
import com.netflix.governator.spi.ModuleTransformer;
import com.netflix.governator.visitors.IsNotStaticInjectionVisitor;
import com.netflix.governator.visitors.KeyTracingVisitor;
import com.netflix.governator.visitors.WarnOfStaticInjectionVisitor;

/**
 * Simple DSL on top of Guice through which an injector may be created using a series
 * of operations and transformations of Guice modules.  Operations are tracked using a 
 * single module and are additive such that each operation executes on top of the entire 
 * current binding state.  Once all bindings have been defined the injector can be created 
 * using an {@link InjectorCreator} strategy.
 * 
 * <code>
 * InjectorBuilder
 *      .fromModule(new MyApplicationModule())
 *      .overrideWith(new OverridesForTesting())
 *      .traceEachElement(new BindingTracingVisitor())
 *      .createInjector();
 * </code>
 */
public final class InjectorBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(InjectorBuilder.class);
    
    private static final Stage LAZY_SINGLETONS_STAGE = Stage.DEVELOPMENT;
    
    private Module module;
    
    /**
     * Start the builder using the specified module. 
     * 
     * @param module
     * @return
     */
    public static InjectorBuilder fromModule(Module module) {
        return new InjectorBuilder(module);
    }
    
    public static InjectorBuilder fromModules(Module ... additionalModules) {
        return new InjectorBuilder(Modules.combine(additionalModules));
    }
    
    public static InjectorBuilder fromModules(List<Module> modules) {
        return new InjectorBuilder(Modules.combine(modules));
    }
    
    private InjectorBuilder(Module module) {
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
    public InjectorBuilder overrideWith(Module ... modules) {
        return overrideWith(Arrays.asList(modules));
    }
    
    /**
     * @see InjectorBuilder#overrideWith(Module...)
     */
    public InjectorBuilder overrideWith(Collection<Module> modules) {
        this.module = Modules.override(module).with(modules);
        return this;
    }
    
    /**
     * Add additional bindings to the module tracked by the DSL
     * @param modules
     */
    public InjectorBuilder combineWith(Module ... modules) {
        List<Module> m = new ArrayList<>();
        m.add(module);
        m.addAll(Arrays.asList(modules));
        this.module = Modules.combine(m);
        return this;
    }
    
    /**
     * Iterator through all elements of the current module and write the output of the
     * ElementVisitor to the logger at debug level.  'null' responses are ignored
     * @param visitor
     */
    public InjectorBuilder traceEachElement(ElementVisitor<String> visitor) {
        for (Element element : Elements.getElements(module)) {
            String message = element.acceptVisitor(visitor);
            if (message != null) {
                LOG.debug(message);
            }
        }
        return this;
    }

    /**
     * Log the current binding state.  traceEachKey() is useful for debugging a sequence of
     * operation where the binding snapshot can be dumped to the log after an operation.
     */
    public InjectorBuilder traceEachKey() {
        return traceEachElement(new KeyTracingVisitor());
    }
    
    /**
     * Log a warning that static injection is being used.  Static injection is considered a 'hack'
     * to alllow for backwards compatibility with non DI'd static code.
     */
    public InjectorBuilder warnOfStaticInjections() {
        return traceEachElement(new WarnOfStaticInjectionVisitor());
    }
    
    /**
     * Extend the core DSL by providing a custom ModuleTransformer.  The output module 
     * replaces the current module.
     * @param transformer
     */
    public InjectorBuilder map(ModuleTransformer transformer) {
        this.module = transformer.transform(module);
        return this;
    }
    
    /**
     * Filter out elements for which the provided visitor returns true.
     * @param predicate
     */
    public InjectorBuilder filter(ElementVisitor<Boolean> predicate) {
        List<Element> elements = new ArrayList<Element>();
        for (Element element : Elements.getElements(module)) {
            if (element.acceptVisitor(predicate)) {
                elements.add(element);
            }
        }
        this.module = Elements.getModule(elements);
        return this;
    }
    
    /**
     * Filter out all bindings using requestStaticInjection
     */
    public InjectorBuilder stripStaticInjections() {
        return filter(new IsNotStaticInjectionVisitor());
    }
    
    /**
     * @return Return all elements in the managed module
     */
    public List<Element> getElements() {
        return Elements.getElements(module);
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
        return creator.createInjector(stage, module);
    }
    
    /**
     * @see {@link InjectorBuilder#createInjector(Stage, InjectorCreator)}
     */
    public <I extends Injector> I createInjector(InjectorCreator<I> creator) {
        return creator.createInjector(LAZY_SINGLETONS_STAGE, module);
    }
    
    /**
     * @see {@link InjectorBuilder#createInjector(Stage, InjectorCreator)}
     */
    public LifecycleInjector createInjector(Stage stage) {
        return createInjector(stage, new LifecycleInjectorCreator());
    }

    /**
     * @see {@link InjectorBuilder#createInjector(Stage, InjectorCreator)}
     */
    public LifecycleInjector createInjector() {
        return createInjector(LAZY_SINGLETONS_STAGE, new LifecycleInjectorCreator());
    }
}
