package com.netflix.governator.guice.jersey;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.servlet.ServletScopes;
import com.google.inject.spi.BindingScopingVisitor;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.core.spi.component.ioc.IoCInstantiatedComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCManagedComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCProxiedComponentProvider;

/**
 * Alternative to Guice's GuiceComponentProviderFactory that does NOT copy Guice bindings into the
 * Jersey configuration
 */
final class GovernatorComponentProviderFactory implements IoCComponentProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernatorComponentProviderFactory.class);
    
    private final Map<Scope, ComponentScope> scopeMap = createScopeMap();
    
    private final Injector injector;

    /**
     * Creates a new GuiceComponentProviderFactory.
     *
     * @param config the resource configuration
     * @param injector the Guice injector
     */
    public GovernatorComponentProviderFactory(ResourceConfig config, Injector injector) {

        if (injector == null) {
            throw new NullPointerException("Guice Injector can not be null!");
        }

        this.injector = injector;
    }

    @Override
    public IoCComponentProvider getComponentProvider(Class<?> c) {
        return getComponentProvider(null, c);
    }

    @Override
    public IoCComponentProvider getComponentProvider(ComponentContext cc, Class<?> clazz) {
        LOGGER.info("getComponentProvider({})", clazz.getName());

        Key<?> key = Key.get(clazz);
        Injector i = findInjector(key);
        // If there is no explicit binding
        if (i == null) {
            // If @Inject is explicitly declared on constructor
            if (isGuiceConstructorInjected(clazz)) {
                try {
                    // If a binding is possible
                    if (injector.getBinding(key) != null) {
                        LOGGER.info("Binding {} to GuiceInstantiatedComponentProvider", clazz.getName());
                        return new GuiceInstantiatedComponentProvider(injector, clazz);
                    }
                } catch (ConfigurationException e) {
                    // The class cannot be injected.
                    // For example, the constructor might contain parameters that
                    // cannot be injected
                    LOGGER.error("Cannot bind " + clazz.getName(), e);
                    // Guice should have picked this up. We fail-fast to prevent
                    // Jersey from trying to handle injection.
                    throw e;
                }
                // If @Inject is declared on field or method
            } else if (isGuiceFieldOrMethodInjected(clazz)) {
                LOGGER.info("Binding {} to GuiceInjectedComponentProvider", clazz.getName());
                return new GuiceInjectedComponentProvider(injector);
            } else {
                return null;
            }
        }

        ComponentScope componentScope = getComponentScope(key, i);
        LOGGER.info("Binding {} to GuiceManagedComponentProvider with the scope \"{}\"",
                new Object[]{clazz.getName(), componentScope});
        return new GuiceManagedComponentProvider(i, componentScope, clazz);
    }

    private ComponentScope getComponentScope(Key<?> key, Injector i) {
        return i.getBinding(key).acceptScopingVisitor(new BindingScopingVisitor<ComponentScope>() {

            @Override
            public ComponentScope visitEagerSingleton() {
                return ComponentScope.Singleton;
            }

            @Override
            public ComponentScope visitScope(Scope theScope) {
                ComponentScope cs = scopeMap.get(theScope);
                return (cs != null) ? cs : ComponentScope.Undefined;
            }

            @Override
            public ComponentScope visitScopeAnnotation(Class scopeAnnotation) {
                // This method is not invoked for Injector bindings
                throw new UnsupportedOperationException();
            }

            @Override
            public ComponentScope visitNoScoping() {
                return ComponentScope.PerRequest;
            }
        });
    }

    private Injector findInjector(Key<?> key) {
        Injector i = injector;
        while (i != null) {
            if (i.getBindings().containsKey(key)) {
                return i;
            }

            i = i.getParent();
        }
        return null;
    }

    /**
     * Determine if a class is an implicit Guice component that can be
     * instantiated by Guice and the life-cycle managed by Jersey.
     *
     * @param c the class.
     * @return true if the class is an implicit Guice component.
     * @deprecated see {@link #isGuiceConstructorInjected(java.lang.Class) }
     */
    @Deprecated
    public boolean isImplicitGuiceComponent(Class<?> c) {
        return isGuiceConstructorInjected(c);
    }

    /**
     * Determine if a class is an implicit Guice component that can be
     * instantiated by Guice and the life-cycle managed by Jersey.
     *
     * @param c the class.
     * @return true if the class is an implicit Guice component.
     */
    public boolean isGuiceConstructorInjected(Class<?> c) {
        for (Constructor<?> con : c.getDeclaredConstructors()) {
            if (isInjectable(con)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determine if a class uses field or method injection via Guice
     * using the {@code Inject} annotation
     *
     * @param c the class.
     * @return true if the class is an implicit Guice component.
     */
    public boolean isGuiceFieldOrMethodInjected(Class<?> c) {
        for (Method m : c.getDeclaredMethods()) {
            if (isInjectable(m)) {
                return true;
            }
        }

        for (Field f : c.getDeclaredFields()) {
            if (isInjectable(f)) {
                return true;
            }
        }

        return !c.equals(Object.class) && isGuiceFieldOrMethodInjected(c.getSuperclass());
    }

    private static boolean isInjectable(AnnotatedElement element) {
        return (element.isAnnotationPresent(com.google.inject.Inject.class)
                || element.isAnnotationPresent(javax.inject.Inject.class));
    }

    /**
     * Maps a Guice scope to a Jersey scope.
     *
     * @return the map
     */
    public Map<Scope, ComponentScope> createScopeMap() {
        Map<Scope, ComponentScope> result = new HashMap<Scope, ComponentScope>();
        result.put(Scopes.SINGLETON, ComponentScope.Singleton);
        result.put(Scopes.NO_SCOPE, ComponentScope.PerRequest);
        result.put(ServletScopes.REQUEST, ComponentScope.PerRequest);
        return result;
    }

    private static class GuiceInjectedComponentProvider
            implements IoCProxiedComponentProvider {

        private final Injector injector;

        public GuiceInjectedComponentProvider(Injector injector) {
            this.injector = injector;
        }

        @Override
        public Object getInstance() {
            throw new IllegalStateException();
        }

        @Override
        public Object proxy(Object o) {
            try {
                injector.injectMembers(o);
            } catch (ProvisionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException)e.getCause();
                }
                throw e;
            }
            return o;
        }
    }

    /**
     * Guice injects instances while Jersey manages their scope.
     *
     * @author Gili Tzabari
     */
    private static class GuiceInstantiatedComponentProvider
            implements IoCInstantiatedComponentProvider {

        private final Injector injector;
        private final Class<?> clazz;

        /**
         * Creates a new GuiceManagedComponentProvider.
         *
         * @param injector the injector
         * @param clazz the class
         */
        public GuiceInstantiatedComponentProvider(Injector injector, Class<?> clazz) {
            this.injector = injector;
            this.clazz = clazz;
        }

        public Class<?> getInjectableClass(Class<?> c) {
            return c.getSuperclass();
        }

        // IoCInstantiatedComponentProvider
        @Override
        public Object getInjectableInstance(Object o) {
            return o;
        }

        @Override
        public Object getInstance() {
            try {
                return injector.getInstance(clazz);
            } catch (ProvisionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException)e.getCause();
                }
                throw e;
            }
        }
    }

    /**
     * Guice injects instances and manages their scope.
     *
     * @author Gili Tzabari
     */
    private static class GuiceManagedComponentProvider extends GuiceInstantiatedComponentProvider
            implements IoCManagedComponentProvider {

        private final ComponentScope scope;

        /**
         * Creates a new GuiceManagedComponentProvider.
         *
         * @param injector the injector
         * @param scope the Jersey scope
         * @param clazz the class
         */
        public GuiceManagedComponentProvider(Injector injector, ComponentScope scope, Class<?> clazz) {
            super(injector, clazz);
            this.scope = scope;
        }

        @Override
        public ComponentScope getScope() {
            return scope;
        }
    }

}
