package com.netflix.governator.guice;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementSource;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;
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
    
    /**
     * List all Module classes that were involved in setting up bindings for the list of Elements
     * @param elements
     * @return
     */
    public static List<String> listModules(List<Element> elements) {
        List<String> names = new ArrayList<>();
        for (Element element : elements) {
            if (element.getSource().getClass().isAssignableFrom(ElementSource.class)) {
                ElementSource source = (ElementSource)element.getSource();
                names.addAll(source.getModuleClassNames());
            }
        }
        return names;
    }
    
    /**
     * List all keys for source and target bindings
     * @param elements
     * @return
     */
    public static Set<Key<?>> listKeys(List<Element> elements) {
        final Set<Key<?>> keys = new HashSet<>();
        for (Element element : elements) {
            element.acceptVisitor(new DefaultElementVisitor<Void>() {
                public <T> Void visit(Binding<T> binding) {
                    keys.add(binding.getKey());
                    binding.acceptTargetVisitor(new DefaultBindingTargetVisitor<T, Void>() {
                        @Override
                        public Void visit(InstanceBinding<? extends T> binding) {
                            keys.add(Key.get(binding.getInstance().getClass()));
                            return null;
                        }

                        @Override
                        public Void visit(LinkedKeyBinding<? extends T> binding) {
                            keys.add(binding.getLinkedKey());
                            return null;
                        }
                    });
                    return null;
                }
            });
        }
        return keys;
    }
}
