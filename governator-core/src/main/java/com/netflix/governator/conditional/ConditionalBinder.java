package com.netflix.governator.conditional;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.internal.Errors;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.Message;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderWithExtensionVisitor;
import com.google.inject.spi.Toolable;

/**
 * An API to bind multiple conditional candidates separately, only to evaluate and 
 * resolve to one candidate when the type is actually injected.  
 * ConditionalBinder is intended for use in a Module
 * 
 * <pre><code>
 * public class SnacksModule extends AbstractModule {
 *   protected void configure() {
 *     ConditionalBinder&lt;Snack&gt; conditionalbinder
 *         = ConditionalBinder.newConditionalBinder(binder(), Snack.class);
 *     multibinder.when(new ConditionalOnProperty("type", "twix")).toInstance(new Twix());
 *     multibinder.when(new ConditionalOnProperty("type", "snickers")).toProvider(SnickersProvider.class);
 *     multibinder.when(new ConditionalOnProperty("type", "skittles")).to(Skittles.class);
 *     multibinder.whenNone().to(Carrots.class);
 *   }
 * }</code></pre>
 *
 * <p>With this binding when injecting {@code <Snack>} all conditionals will be evaluated and only
 * the single matched binding will be injected
 * <pre><code>
 * class SnackMachine {
 *   {@literal @}Inject
 *   public SnackMachine(Snack snacks) { ... }
 * }</code></pre>
 *
 * <p>Contributing conditional bindings from different modules is supported. For
 * example, it is okay for both {@code CandyModule} and {@code ChipsModule}
 * to create their own {@code ConditionalBinder<Snack>}, and to each contribute
 * conditional bindings to the set of candidate snacks.  When a Snack is injected, it will 
 * use the one conditional bindings that matched.
 *
 * Exactly one conditional binding may be matched.  An exception will be thrown 
 * when Snack is injected and no or multiple bindings' conditions are matched.
 * 
 * <p>Conditionals are evaluated at injection time. If an element is bound to a
 * provider, that provider's get method will be called each time Snack is
 * injected (unless the binding is in Singleton scope, in which case Guice will cache
 * the first call to get).
 * 
 * Conditionals may only be used in Stage.DEVELOPMENT since running in Stage.PRODUCTION will
 * result in every Singleton conditional candidate being instantiated eagerly.  Any attempt
 * to binding in Stage.PRODUCTION will result in a CreationException
 * 
 * TODO: Strip all conditional bindings of their scope so they cannot be eager singletons.
 *       Alternatively, force lazy singleton behavior
 * TODO: Discuss whether the conditional binder key should be singleton by default  
 */
public abstract class ConditionalBinder<T> {
    /**
     * Returns a new ConditionalBinder that tracks all candidate instances of {@code type}.
     */
    public static <T> ConditionalBinder<T> newConditionalBinder(Binder binder, TypeLiteral<T> type) {
        return newRealConditionalBinder(binder, Key.get(type));
    }

    /**
     * Returns a new ConditionalBinder that tracks all candidate instances of {@code type}.
     */
    public static <T> ConditionalBinder<T> newConditionalBinder(Binder binder, Class<T> type) {
        return newRealConditionalBinder(binder, Key.get(type));
    }

    /**
     * Returns a new ConditionalBinder that tracks all candidate instances of {@code type} with 
     * the qualifier {@code annotation}
     */
    public static <T> ConditionalBinder<T> newConditionalBinder(
        Binder binder, TypeLiteral<T> type, Annotation annotation) {
        return newRealConditionalBinder(binder, Key.get(type, annotation));
    }

    /**
     * Returns a new ConditionalBinder that tracks all candidate instances of {@code type} with 
     * the qualifier {@code annotation}
     */
    public static <T> ConditionalBinder<T> newConditionalBinder(
        Binder binder, Class<T> type, Annotation annotation) {
        return newRealConditionalBinder(binder, Key.get(type, annotation));
    }

    /**
     * Returns a new ConditionalBinder that tracks all candidate instances of {@code type} with 
     * the qualifier {@code annotation}
     */
    public static <T> ConditionalBinder<T> newConditionalBinder(Binder binder, TypeLiteral<T> type,
        Class<? extends Annotation> annotationType) {
        return newRealConditionalBinder(binder, Key.get(type, annotationType));
    }

    /**
     * Returns a new ConditionalBinder that tracks all candidate instances of {@code key} where
     * key may or may not have a qualifier.
     */
    public static <T> ConditionalBinder<T> newConditionalBinder(Binder binder, Key<T> key) {
        return newRealConditionalBinder(binder, key);
    }    
  
    /**
     * Returns a new ConditionalBinder that tracks all candidate instances of {@code type} with 
     * the qualifier {@code annotation}
     */
    public static <T> ConditionalBinder<T> newConditionalBinder(Binder binder, Class<T> type,
            Class<? extends Annotation> annotationType) {
        return newRealConditionalBinder(binder, Key.get(type, annotationType));
    }

    static <T> ConditionalBinder<T> newRealConditionalBinder(Binder binder, Key<T> key) {
        if (binder.currentStage().equals(Stage.PRODUCTION)) {
            throw new RuntimeException("ConditionalBinder may not be used in Stage.PRODUCTION.  Use Stage.DEVELOPMENT.");
        }

        binder = binder.skipSources(RealConditionalBinder.class, ConditionalBinder.class);
        RealConditionalBinder<T> result = new RealConditionalBinder<T>(binder, key);
        binder.install(result);
        return result;
    }
    
    /**
     * Returns a binding builder used to add a new conditional candidate for the key. 
     * Each bound element must have a distinct value. Only the matching candidate will
     * be instantiated and its provider cached when the key is first injected.
     *
     * <p>It is an error to call this method without also calling one of the
     * {@code to} methods on the returned binding builder.
     *
     * <p>Scoping elements independently supported per binding. Use the {@code in} method
     * to specify a binding scope.
     */
    public abstract LinkedBindingBuilder<T> whenMatch(Conditional obj);

    /**
     * Returns a binding builder used to specify the candidate for the key when no other
     * conditional bindings have been met.  There can be only one default candidate.  Only the
     * matching candidate will be instantiated and its provider cached when the key is 
     * first injected.
     *
     * <p>It is an error to call this method without also calling one of the
     * {@code to} methods on the returned binding builder.
     *
     * <p>Scoping elements independently supported per binding. Use the {@code in} method
     * to specify a binding scope.
     */
    public abstract LinkedBindingBuilder<T> whenNoMatch();
    
    static final class RealConditionalBinder<T> extends ConditionalBinder<T>
        implements Module, ProviderWithExtensionVisitor<T>, ConditionalBinding<T> {
        
        private final TypeLiteral<T> elementType;
        private final String keyName;
        private final Key<T> primaryKey;
        private ImmutableList<Binding<T>> candidateBindings;
        private Set<Dependency<?>> dependencies;

        private Provider<T> matchedProvider;
        private Binder binder;
        
        public RealConditionalBinder(
                Binder binder,
                Key<T> key) {
            this.binder = checkNotNull(binder, "binder");
            this.primaryKey = checkNotNull(key, "key");
            this.elementType = checkNotNull(key.getTypeLiteral(), "elementType");
            this.keyName = checkNotNull(ConditionalElementImpl.nameOf(key), "keyName");
        }
        
        @Override
        public void configure(Binder binder) {
            checkConfiguration(!isInitialized(), "ConditionalBinder was already initialized");
            binder.bind(primaryKey).toProvider(this);
        }
        
        Key<T> getKeyForNewItem(Conditional obj) {
            checkConfiguration(!isInitialized(), "ConditionalBinder was already initialized");
            return Key.get(elementType, new ConditionalElementImpl(keyName, obj));
        }

        @Override 
        public LinkedBindingBuilder<T> whenMatch(Conditional obj) {
            return binder.bind(getKeyForNewItem(obj));
        }

        @Override
        public LinkedBindingBuilder<T> whenNoMatch() {
            return binder.bind(getKeyForNewItem(null));
        }

        /**
         * Invoked by Guice at Injector-creation time to prepare providers for each
         * element in this set. 
         */
        @Toolable 
        @Inject 
        void initialize(Injector injector) {
            List<Binding<T>> candidateBindings = Lists.newArrayList();
            Binding<T> matchedBinding = null;
            Binding<T> defaultBinding = null;
            
            Set<Indexer.IndexedBinding> index = Sets.newHashSet();
            Indexer indexer = new Indexer(injector);
            List<Dependency<?>> dependencies = Lists.newArrayList();
            
            for (Binding<?> entry : injector.findBindingsByType(elementType)) {
                if (keyMatches(entry.getKey())) {
                    @SuppressWarnings("unchecked") // protected by findBindingsByType()
                    Binding<T> binding = (Binding<T>) entry;
                    if (index.add(binding.acceptTargetVisitor(indexer))) {
                        candidateBindings.add(binding);
                        
                        Conditional condition = ((ConditionalElementImpl) entry.getKey().getAnnotation()).getCondition();
                        if (condition == null) {
                            if (defaultBinding == null) {
                                defaultBinding = binding;
                            }
                            else {
                                throw newDuplicateBindingException(primaryKey, defaultBinding, binding);
                            }
                        }
                        else {
                            @SuppressWarnings("unchecked")
                            Class matcherType = condition.getMatcherClass();
                            Matcher matcher = (Matcher)injector.getInstance(matcherType);
                            if (matcher.match(condition)) {
                                if (matchedBinding == null) {
                                    matchedBinding = binding;
                                }
                                else {
                                    throw newDuplicateBindingException(primaryKey, matchedBinding, binding);
                                }
                            }
                        }
                        dependencies.add(Dependency.get(binding.getKey()));
                    }
                }
            }
            
            if (matchedBinding == null) {
                matchedBinding = defaultBinding;
            }
            
            if (matchedBinding == null) {
                throw newNoMatchingBindingException(primaryKey, candidateBindings);
            }
            
            dependencies.add(Dependency.get(matchedBinding.getKey()));
            this.matchedProvider = matchedBinding.getProvider();
            
            this.candidateBindings = ImmutableList.copyOf(candidateBindings);
            this.binder = null;
        }

        private boolean keyMatches(Key<?> key) {
            return key.getTypeLiteral().equals(elementType)
                && key.getAnnotation() instanceof ConditionalElement
                && ((ConditionalElement) key.getAnnotation()).keyName().equals(keyName);
        }

        @Override
        public T get() {
            checkConfiguration(isInitialized(), "ConditionalBinder is not initialized");
            return matchedProvider.get();
        }
        
        @Override
        public Key<T> getKey() {
            return primaryKey;
        }
        
        @Override
        public List<Binding<?>> getCandidateElements() {
            if (isInitialized()) {
                return (List<Binding<?>>) (List<?>) candidateBindings; // safe because bindings is immutable.
            } 
            else {
                throw new UnsupportedOperationException("getElements() not supported for module bindings");
            }
        }
        
        @Override
        public <B, V> V acceptExtensionVisitor(
                BindingTargetVisitor<B, V> visitor,
                ProviderInstanceBinding<? extends B> binding) {
            if (visitor instanceof ConditionalBindingsTargetVisitor) {
                return ((ConditionalBindingsTargetVisitor<T, V>) visitor).visit(this);
            } 
            else {
                return visitor.visit(binding);
            }
        }
        
        private boolean isInitialized() {
            return binder == null;
        }

        @Override
        public boolean containsElement(com.google.inject.spi.Element element) {
            if (element instanceof Binding) {
                Binding<?> binding = (Binding<?>) element;
                return keyMatches(binding.getKey())
                  || binding.getKey().equals(primaryKey);
            } 
            else {
                return false;
            }
        }
        
        @Override
        public int hashCode() {
            return primaryKey.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RealConditionalBinder other = (RealConditionalBinder) obj;
            return primaryKey.equals(other.primaryKey);
        }
    }

    static void checkConfiguration(boolean condition, String format, Object... args) {
        if (condition) {
            return;
        }

        throw new ConfigurationException(ImmutableSet.of(new Message(Errors.format(format, args))));
    }
    
    private static <T> ConfigurationException newDuplicateBindingException(
            Key<T> key,
            Binding<T> existingBindings,
            Binding<T> duplicateBinding) {
        // When the value strings don't match, include them both as they may be useful for debugging
        return new ConfigurationException(ImmutableSet.of(new Message(Errors.format(
                "%s injection failed due to multiple matching conditionals:"
                    + "\n    \"%s\"\n        bound at %s"
                    + "\n    \"%s\"\n        bound at %s",
                key,
                duplicateBinding.getKey(),
                duplicateBinding.getSource(),
                existingBindings.getKey(),
                existingBindings.getSource()))));
    }

    private static <T> ConfigurationException newNoMatchingBindingException(
            Key<T> key,
            List<Binding<T>> candidateBindings) {
        
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s injection failed due to no matching conditional", key));
        
        if (!candidateBindings.isEmpty()) {
            for (Binding<T> binding : candidateBindings) {
                builder.append(String.format("\n    \"%s\"\n        bound at %s", 
                        binding.getKey(),
                        binding.getSource()));
            }
        }
        else {
            builder.append("\n    No to() bindings were specified ");
        }
        return new ConfigurationException(ImmutableSet.of(new Message(Errors.format(builder.toString()))));
    }

    static <T> T checkNotNull(T reference, String name) {
        if (reference != null) {
            return reference;
        }

        NullPointerException npe = new NullPointerException(name);
        throw new ConfigurationException(ImmutableSet.of(new Message(npe
                .toString(), npe)));
    }
}
