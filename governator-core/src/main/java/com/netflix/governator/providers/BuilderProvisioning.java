package com.netflix.governator.providers;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Provider;

import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.BindingAnnotation;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.HasDependencies;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.ModuleAnnotatedMethodScanner;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderWithExtensionVisitor;
import com.google.inject.spi.Toolable;
import com.netflix.governator.annotations.binding.BuilderAdvice;
import com.netflix.governator.annotations.binding.ProvidesBuilder;

public final class BuilderProvisioning {

    private BuilderProvisioning() {
    }

    /**
     * delegates to an existing Provider<X>, then invokes a set of Consumer<X> on the result.
     */
    private static class BuilderProvider<X> implements ProviderWithExtensionVisitor<X>, HasDependencies {
        static final Type CONSUMER_TYPE = Consumer.class.getTypeParameters()[0];
        private final String name;
        private final Key<X> delegateKey;
        private Provider<X> delegateProvider;
        private final TypeToken<?> providedTypeToken;
        private final Set<Dependency<?>> dependencies = new HashSet<>();
        private final Set<Binding<Consumer<X>>> consumerBindings = new LinkedHashSet<>();
        private final Predicate<Binding<Consumer<X>>> consumerBindingFilter;
        private final ConsumerTypeMatcher consumerMatcher = new ConsumerTypeMatcher();


        public BuilderProvider(String name, TypeLiteral<X> providedType, Provider<X> delegate,
                Predicate<Binding<Consumer<X>>> consumerBindingFilter) {
            this.name = name;
            this.delegateProvider = delegate;
            this.providedTypeToken = TypeToken.of(providedType.getType());
            this.delegateKey = null;
            this.consumerBindingFilter = (consumerBindingFilter != null) ? consumerBindingFilter : b -> true;
        }

        public BuilderProvider(String name, TypeLiteral<X> providedType, Key<X> delegateKey,
                Predicate<Binding<Consumer<X>>> consumerBindingFilter) {
            this.name = name;
            this.providedTypeToken = TypeToken.of(providedType.getType());
            this.delegateKey = delegateKey;
            this.consumerBindingFilter = (consumerBindingFilter != null) ? consumerBindingFilter : b -> true;
        }

        @Override
        public X get() {
            X provided = delegateProvider.get();
            for (Binding<Consumer<X>> advisorBinding : consumerBindings) {
                advisorBinding.getProvider().get().accept(provided);
            }
            return provided;
        }

        /**
         * check bindings for consumers of this builder (Consumer<X>)
         */
        @SuppressWarnings("unchecked")
        @Toolable
        @javax.inject.Inject
        protected void initialize(Injector injector) {
            for (Binding<?> binding : injector.getAllBindings().values()) {
                Key<?> bindingKey = binding.getKey();
                if (bindingKey.hasAttributes()
                        && BuilderElement.class.isAssignableFrom(bindingKey.getAnnotationType())) {
                    BuilderElement builderElement = (BuilderElement) bindingKey.getAnnotation();
                    if (builderElement.type() == BuilderElement.Type.CONSUMER
                            && builderElement.builderName().equals(name)) {
                        if (consumerMatcher.matches(binding)) {
                            Binding<Consumer<X>> consumerBinding = (Binding<Consumer<X>>) binding;
                            if (consumerBindingFilter.test(consumerBinding)) {
                                consumerBindings.add(consumerBinding);
                                dependencies.add(Dependency.get(bindingKey));
                            }
                        }
                    }
                    if (builderElement.type() == BuilderElement.Type.PROVIDER
                            && builderElement.builderName().equals(name)) {
                        if (delegateProvider == null && delegateKey != null) {
                            if (bindingKey.equals(delegateKey)) {
                                delegateProvider = (Provider<X>) binding.getProvider();
                                dependencies.add(Dependency.get(bindingKey));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public <B, V> V acceptExtensionVisitor(BindingTargetVisitor<B, V> visitor,
                ProviderInstanceBinding<? extends B> binding) {
            return visitor.visit(binding);
        }

        @Override
        public Set<Dependency<?>> getDependencies() {
            return dependencies;
        }

        /**
         * matches TypeLiterals of type Consumer<X>
         * 
         * @author tcellucci
         *
         */
        final class ConsumerTypeMatcher extends AbstractMatcher<Binding<?>> {
            @Override
            public boolean matches(Binding<?> binding) {
                TypeLiteral<?> bindingType = binding.getKey().getTypeLiteral();
                TypeToken<?> consumerTypeToken = TypeToken.of(bindingType.getType())
                        .resolveType(CONSUMER_TYPE);
                return consumerTypeToken.isSupertypeOf(providedTypeToken);
            }
        }
    }

    /**
     * scans injector for module methods annotated with @ProvidesBuilder
     *
     */
    private static class ProvidesBuilderScanner extends ModuleAnnotatedMethodScanner {
        private static final Type CONSUMED_TYPE = Consumer.class.getTypeParameters()[0];

        @Override
        public <X> Key<X> prepareMethod(Binder binder, Annotation annotation, Key<X> key,
                InjectionPoint injectionPoint) {
            if (annotation instanceof ProvidesBuilder) {
                TypeLiteral<X> providedTypeLiteral = key.getTypeLiteral();
                String builderName = ((ProvidesBuilder) annotation).value();
                Key<X> newKey = Key.get(providedTypeLiteral,
                        new _BuildElement(builderName, BuilderElement.Type.PROVIDER));
                binder.bind(key)
                        .toProvider(new BuilderProvider<X>(builderName, providedTypeLiteral, binder.getProvider(newKey),
                                null));
                return newKey;
            }
            else if (annotation instanceof BuilderAdvice) {                
                TypeLiteral<Consumer<?>> consumerTypeLiteral = (TypeLiteral<Consumer<?>>) key.getTypeLiteral();
                String builderName = ((BuilderAdvice) annotation).value();
                Key<X> newKey = Key.get(key.getTypeLiteral(),
                        new _BuildElement(builderName, BuilderElement.Type.CONSUMER));
                return newKey;
            }
            else {
                return key;
            }
        }

        @Override
        public Set<? extends Class<? extends Annotation>> annotationClasses() {
            return new HashSet<>(Arrays.asList(ProvidesBuilder.class, BuilderAdvice.class));
        }

        /*
         * pseudo-annotation for generating unique binding key at runtime
         */
        private final static class _BuildElement implements BuilderElement {
            private static final AtomicInteger nextUniqueId = new AtomicInteger(1);

            private final int uniqueId;
            private final BuilderElement.Type type;
            private final String builderName;

            _BuildElement(String builderName, Type type) {
                this.builderName = builderName;
                this.type = type;
                this.uniqueId = nextUniqueId.getAndIncrement();
            }

            public int uniqueId() {
                return uniqueId;
            }

            public String builderName() {
                return builderName;
            }

            public Type type() {
                return type;
            }

            public Class<? extends Annotation> annotationType() {
                return BuilderElement.class;
            }

            @Override
            public String toString() {
                return "@" + BuilderElement.class.getName()
                        + "(builderName=" + builderName + ")"
                        + "(uniqueId=" + uniqueId + ")"
                        + "(type=" + type + ")";
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof BuilderElement
                        && ((BuilderElement) o).builderName().equals(builderName())
                        && ((BuilderElement) o).uniqueId() == uniqueId()
                        && ((BuilderElement) o).type() == type();
            }

            @Override
            public int hashCode() {
                return ((127 * "builderName".hashCode()) ^ builderName.hashCode())
                        + ((127 * "uniqueId".hashCode()) ^ uniqueId)
                        + ((127 * "type".hashCode()) ^ type.hashCode());
            }
        }
    }

    @Retention(RUNTIME)
    @BindingAnnotation
    private abstract @interface BuilderElement {
        enum Type {
            PROVIDER, CONSUMER
        }

        int uniqueId();

        String builderName() default "";

        Type type();
    }

    public static <X> LinkedBindingBuilder<X> bindBuilder(Binder binder, String builderName, Key<X> providerKey) {
        return bindBuilder(binder, builderName, providerKey, null);
    }

    public static void bind(Binder binder) {
        binder.install(new BuilderProvisioningModule());
    }

    public static <X> LinkedBindingBuilder<X> bindBuilder(Binder binder, String builderName, Key<X> providerKey,
            Predicate<Binding<Consumer<X>>> consumerBindingFilter) {
        bind(binder);
        Key<X> newKey = Key.get(providerKey.getTypeLiteral(),
                new ProvidesBuilderScanner._BuildElement(builderName, BuilderElement.Type.PROVIDER));
        BuilderProvider<X> provider = new BuilderProvider<>(builderName, providerKey.getTypeLiteral(), newKey,
                consumerBindingFilter);
        binder.bind(providerKey).toProvider(provider);
        return binder.bind(newKey);
    }

    public static <X> LinkedBindingBuilder<Consumer<X>> bindAdvice(Binder binder, String builderName, Key<Consumer<X>> providerKey) {
        bind(binder);
        Key<Consumer<X>> newKey = Key.get(providerKey.getTypeLiteral(),
                new ProvidesBuilderScanner._BuildElement(builderName, BuilderElement.Type.CONSUMER));
        return binder.bind(newKey);
    }

    private final static class BuilderProvisioningModule extends AbstractModule {

        @Override
        public void configure() {
            binder().scanModulesForAnnotatedMethods(new ProvidesBuilderScanner());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && getClass() == obj.getClass();
        }
    }

}
