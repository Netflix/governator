package com.netflix.governator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Binding;
import com.google.inject.ConfigurationException;
import com.google.inject.Key;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementSource;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;

/**
 * 
 * @deprecated Moved to karyon
 */
@Deprecated
public abstract class ElementsEx {
    /**
     * List all Module classes that were involved in setting up bindings for the list of Elements
     * @param elements
     * @return
     */
    public static List<String> getAllSourceModules(List<Element> elements) {
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
    public static Set<Key<?>> getAllBoundKeys(List<Element> elements) {
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
    
    public static Set<Key<?>> getAllInjectionKeys(List<Element> elements) {
        final Set<Key<?>> keys = new HashSet<>();
        for (Key<?> key : getAllBoundKeys(elements)) {
            keys.add(key);
            try {
                for (Dependency d : InjectionPoint.forConstructorOf(key.getTypeLiteral()).getDependencies()) {
                    keys.add(d.getKey());
                }
            }
            catch (ConfigurationException e) {
                
            }
            if (key.getTypeLiteral() != null) {
                if (!key.getTypeLiteral().getRawType().isInterface()) {
                    for (InjectionPoint ip : InjectionPoint.forInstanceMethodsAndFields(key.getTypeLiteral())) {
                        for (Dependency d : ip.getDependencies()) {
                            keys.add(d.getKey());
                        }
                    }
                }
            }
        }
        return keys;
    }
}
