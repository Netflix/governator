package com.netflix.governator.guice.actions;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderKeyBinding;
import com.google.inject.spi.UntargettedBinding;
import com.netflix.governator.guice.PostInjectorAction;

public class BindingReport implements PostInjectorAction {
    private static final Logger LOG = LoggerFactory.getLogger(BindingReport.class);
            
    private final String label;
    
    public BindingReport(String label) {
        this.label = label;
    }
    
    public BindingReport() {
        this(">>>> GUICE BINDING REPORT <<<<");
    }
    
    @Override
    public void call(Injector injector) {
        LOG.info("Bindings for " + label);
        LOG.info(describeBindings("Binding  : ", injector.getBindings().entrySet()));
        
        Map<Key<?>, Binding<?>> jitBindings = Maps.difference(injector.getAllBindings(), injector.getBindings()).entriesOnlyOnLeft();
        LOG.info(describeBindings("JIT      : ", jitBindings.entrySet()));
        
    }

    private String describeBindings(final String label, Set<Entry<Key<?>, Binding<?>>> bindings) {
        final StringBuilder sb = new StringBuilder();
        for (Entry<Key<?>, Binding<?>> binding : bindings) {
            binding.getValue().acceptVisitor(new DefaultElementVisitor<Void>() {
                @Override
                public <T> Void visit(Binding<T> binding) {
                    sb.append("\n" + label  + binding.getKey()).append("\n");
                    if (binding.getSource() != null) {
                        sb.append("   where : " + binding.getSource()).append("\n");;
                    }
                    binding.acceptTargetVisitor(new DefaultBindingTargetVisitor<T, Void>() {
                        public Void visit(UntargettedBinding<? extends T> untargettedBinding) {
                            sb.append(describeInjectionPoints(untargettedBinding.getKey().getTypeLiteral()));
                            return null;
                        }
                        
                        @Override
                        public Void visit(InstanceBinding<? extends T> binding) {
                            sb.append("  to (I) : " + binding.getInstance().getClass()).append("\n");
                            sb.append(describeInjectionPoints(TypeLiteral.get(binding.getInstance().getClass())));
                            return null;
                        }

                        @Override
                        public Void visit(ProviderInstanceBinding<? extends T> binding) {
                            sb.append("  to (P) : " + binding.getProviderInstance().getClass()).append("\n");
                            sb.append(describeInjectionPoints(TypeLiteral.get(binding.getProviderInstance().getClass())));
                            return null;
                        }

                        @Override
                        public Void visit(ProviderKeyBinding<? extends T> binding) {
                            sb.append("  to (P) : " + binding.getProviderKey()).append("\n");
                            sb.append(describeInjectionPoints(binding.getProviderKey().getTypeLiteral()));
                            return null;
                        }

                        @Override
                        public Void visit(LinkedKeyBinding<? extends T> binding) {
                            sb.append("  to (I) : " + binding.getLinkedKey()).append("\n");
                            sb.append(describeInjectionPoints(binding.getLinkedKey().getTypeLiteral()));
                            return null;
                        }

                        @Override
                        public Void visit(ProviderBinding<? extends T> binding) {
                            sb.append("  to (P) : " + binding.getProvidedKey()).append("\n");
                            sb.append(describeInjectionPoints(binding.getProvidedKey().getTypeLiteral()));
                            return null;
                        }
                    });
                    sb.append(describeInjectionPoints(binding.getKey().getTypeLiteral()));
                    
                    return null;
                }
            });
        }
        return sb.toString();
    }
    
    private String describeInjectionPoints(TypeLiteral<?> type) {
        StringBuilder sb = new StringBuilder();
        
        try {
            InjectionPoint ip = InjectionPoint.forConstructorOf(type);
            List<Dependency<?>> deps = ip.getDependencies();
            if (!deps.isEmpty()) {
                for (Dependency<?> dep : deps) {
                    sb.append("     dep : " + dep.getKey()).append("\n");
                }
            }
        }
        catch (Exception e) {
            
        }

        try {
            Set<InjectionPoint> ips = InjectionPoint.forInstanceMethodsAndFields(type);
            for (InjectionPoint ip2 : ips) {
                List<Dependency<?>> deps = ip2.getDependencies();
                if (!deps.isEmpty()) {
                    for (Dependency<?> dep : deps) {
                        sb.append("     mem : " + dep.getKey()).append("\n");
                    }
                }
            }
        }
        catch (Exception e) {
            
        }

        return sb.toString();
    }
}
