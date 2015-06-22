package com.netflix.governator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementSource;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;

public abstract class ElementsEx {
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
