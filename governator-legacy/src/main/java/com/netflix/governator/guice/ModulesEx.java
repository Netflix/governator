package com.netflix.governator.guice;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.netflix.governator.guice.annotations.Bootstrap;

/**
 * Utility class similar to Guice's Modules that simplifies recipes for
 * combing Guice modules.
 * 
 * @author elandau
 *
 */
public class ModulesEx {
    private static final Logger LOG = LoggerFactory.getLogger(ModulesEx.class);
    
    public static Module combineAndOverride(Module ... modules) {
        return combineAndOverride(Arrays.asList(modules));
    }
    
    /**
     * Generate a single module that is produced by accumulating and overriding
     * each module with the next.
     * 
     * <pre>
     * {@code 
     * Guice.createInjector(ModuleUtils.combineAndOverride(moduleA, moduleAOverrides, moduleB));
     * }
     * </pre>
     * 
     * @param modules
     * @return
     */
    public static Module combineAndOverride(List<? extends Module> modules) {
        Iterator<? extends Module> iter = modules.iterator();
        Module current = Modules.EMPTY_MODULE;
        if (iter.hasNext()) {
            current = iter.next();
            if (iter.hasNext()) {
                current = Modules.override(current).with(iter.next());
            }
        }
        
        return current;
    }
    
    public static Module fromClass(final Class<?> cls) {
        return fromClass(cls, true);
    }
    
    /**
     * Create a single module that derived from all bootstrap annotations
     * on a class, where that class itself is a module.
     * 
     * For example,
     * <pre>
     * {@code 
     *    public class MainApplicationModule extends AbstractModule {
     *        @Override
     *        public void configure() {
     *            // Application specific bindings here
     *        }
     *        
     *        public static void main(String[] args) {
     *            Guice.createInjector(ModulesEx.fromClass(MainApplicationModule.class));
     *        }
     *    }
     * }
     * </pre>
     * @author elandau
     */
    public static Module fromClass(final Class<?> cls, final boolean override) {
        List<Module> modules = new ArrayList<>();
        // Iterate through all annotations of the main class, create a binding for the annotation
        // and add the module to the list of modules to install
        for (final Annotation annot : cls.getDeclaredAnnotations()) {
            final Class<? extends Annotation> type = annot.annotationType();
            Bootstrap bootstrap = type.getAnnotation(Bootstrap.class);
            if (bootstrap != null) {
                LOG.info("Adding Module {}", bootstrap.module());
                try {
                    modules.add(bootstrap.module().getConstructor(type).newInstance(annot));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        
        try {
            if (override) {
                return Modules.override(combineAndOverride(modules)).with((Module)cls.newInstance());
            }
            else {
                return Modules.combine(Modules.combine(modules), (Module)cls.newInstance());
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
