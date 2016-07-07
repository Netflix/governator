/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.lifecycle;

import java.beans.Introspector;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.naming.NamingException;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.netflix.governator.LifecycleAction;
import com.netflix.governator.annotations.PreConfiguration;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.configuration.ConfigurationColumnWriter;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationMapper;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.guice.PostInjectorAction;
import com.netflix.governator.internal.JSR250LifecycleAction.ValidationMode;
import com.netflix.governator.internal.PreDestroyLifecycleFeature;
import com.netflix.governator.internal.PreDestroyMonitor;

/**
 * Main instance management container
 */
@Singleton
public class LifecycleManager implements Closeable, PostInjectorAction
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<Object, LifecycleState> objectStates = new MapMaker().weakKeys().makeMap();
    private final PreDestroyLifecycleFeature preDestroyLifecycleFeature = new PreDestroyLifecycleFeature(ValidationMode.LAX);
    private final AtomicReference<State> state = new AtomicReference<State>(State.LATENT);
    private final ConfigurationDocumentation configurationDocumentation;
    private final ConfigurationProvider configurationProvider;
    private final ConfigurationMapper configurationMapper;
    private final Collection<LifecycleListener> listeners;
    private final Collection<ResourceLocator> resourceLocators;
    private final ValidatorFactory factory;
    private final Injector injector;
    private final PreDestroyMonitor preDestroyMonitor;
    private com.netflix.governator.LifecycleManager newLifecycleManager;

    public LifecycleManager()
    {
        this(new LifecycleManagerArguments(), null);
    }

    public LifecycleManager(LifecycleManagerArguments arguments)
    {
        this(arguments, null);
    }

    @Inject
    public LifecycleManager(LifecycleManagerArguments arguments, Injector injector)
    {
        this.injector = injector;
        if (injector != null) {
            preDestroyMonitor =  new PreDestroyMonitor(injector.getScopeBindings());
        }
        else {
            preDestroyMonitor = null;
        }
        configurationMapper = arguments.getConfigurationMapper();
        newLifecycleManager = arguments.getLifecycleManager();
        listeners = ImmutableSet.copyOf(arguments.getLifecycleListeners());
        resourceLocators = ImmutableSet.copyOf(arguments.getResourceLocators());
        factory = Validation.buildDefaultValidatorFactory();
        configurationDocumentation = arguments.getConfigurationDocumentation();
        configurationProvider = arguments.getConfigurationProvider();
    }

    /**
     * Return the lifecycle listener if any
     *
     * @return listener or null
     */
    public Collection<LifecycleListener> getListeners()
    {
        return listeners;
    }

    /**
     * Add the objects to the container. Their assets will be loaded, post construct methods called, etc.
     *
     * @param objects objects to add
     * @throws Exception errors
     */
    @Deprecated
    public void add(Object... objects) throws Exception
    {
        for ( Object obj : objects )
        {
            add(obj);
        }
    }

    /**
     * Add the object to the container. Its assets will be loaded, post construct methods called, etc.
     *
     * @param obj object to add
     * @throws Exception errors
     */
    @Deprecated
    public void add(Object obj) throws Exception
    {
        add(obj, null, new LifecycleMethods(obj.getClass()));
    }

    /**
     * Add the object to the container. Its assets will be loaded, post construct methods called, etc.
     * This version helps performance when the lifecycle methods have already been calculated
     *
     * @param obj     object to add
     * @param methods calculated lifecycle methods
     * @throws Exception errors
     */
    @Deprecated
    public void add(Object obj, LifecycleMethods methods) throws Exception
    {
        Preconditions.checkState(state.get() != State.CLOSED, "LifecycleManager is closed");

        startInstance(obj, null, methods);

        if ( hasStarted() )
        {
            initializeObjectPostStart(obj);
        }
    }
    
    /**
     * Add the object to the container. Its assets will be loaded, post construct methods called, etc.
     * This version helps performance when the lifecycle methods have already been calculated
     *
     * @param obj     object to add
     * @param methods calculated lifecycle methods
     * @throws Exception errors
     */
    public <T> void add(T obj, Binding<T> binding, LifecycleMethods methods) throws Exception
    {
        Preconditions.checkState(state.get() != State.CLOSED, "LifecycleManager is closed");

        startInstance(obj, binding, methods);

        if ( hasStarted() )
        {
            initializeObjectPostStart(obj);
        }
    }
    

    /**
     * Returns true if the lifecycle has started (i.e. {@link #start()} has been called).
     *
     * @return true/false
     */
    public boolean hasStarted()
    {
        return state.get() == State.STARTED;
    }

    /**
     * Return the current state of the given object or LATENT if unknown
     *
     * @param obj object to check
     * @return state
     */
    public LifecycleState getState(Object obj)
    {
        LifecycleState lifecycleState = objectStates.get(obj);
        if ( lifecycleState == null )
        {
            lifecycleState = hasStarted() ? LifecycleState.ACTIVE : LifecycleState.LATENT;
        }
        return lifecycleState;
    }

    /**
     * The manager MUST be started. Note: this method
     * waits indefinitely for warm up methods to complete
     *
     * @throws Exception errors
     */
    public void start() throws Exception
    {
        start(0, null);
    }

    /**
     * The manager MUST be started. This version of start() has a maximum
     * wait period for warm up methods.
     *
     * @param maxWait maximum wait time for warm up methods - if the time elapses, the warm up methods are interrupted
     * @param unit    time unit
     * @return true if warm up methods successfully executed, false if the time elapses
     * @throws Exception errors
     */
    @Deprecated
    public boolean start(long maxWait, TimeUnit unit) throws Exception
    {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTING), "Already started");

        validate();

        new ConfigurationColumnWriter(configurationDocumentation).output(log);
        if (newLifecycleManager != null) {
            newLifecycleManager.notifyStarted();
        }
        state.set(State.STARTED);

        return true;
    }

    @Override
    public synchronized void close()
    {
        if ( state.compareAndSet(State.STARTING, State.CLOSED) || state.compareAndSet(State.STARTED, State.CLOSED) )
        {
            try
            {
                if (newLifecycleManager != null) {
                    newLifecycleManager.notifyShutdown();
                }
                stopInstances();
            }
            catch ( Exception e )
            {
                log.error("While stopping instances", e);
            }
            finally
            {
                objectStates.clear();
            }
        }
    }

    /**
     * Run the validations on the managed objects. This is done automatically when {@link #start()} is called.
     * But you can call this at any time you need.
     *
     * @throws ValidationException
     */
    public void validate() throws ValidationException
    {
        ValidationException exception = null;
        Validator validator = factory.getValidator();
        for ( Object managedInstance : objectStates.keySet() )
        {
            if (managedInstance != null) {
                exception = internalValidateObject(exception, managedInstance, validator);
            }
        }

        if ( exception != null )
        {
            throw exception;
        }
    }

    /**
     * Run validations on the given object
     *
     * @param obj the object to validate
     * @throws ValidationException
     */
    public void validate(Object obj) throws ValidationException
    {
        Validator validator = factory.getValidator();
        ValidationException exception = internalValidateObject(null, obj, validator);
        if ( exception != null )
        {
            throw exception;
        }
    }

    private void setState(Object obj, LifecycleState state)
    {
        objectStates.put(obj, state);
        for ( LifecycleListener listener : listeners )
        {
            listener.stateChanged(obj, state);
        }
    }

    private ValidationException internalValidateObject(ValidationException exception, Object obj, Validator validator)
    {
        Set<ConstraintViolation<Object>> violations = validator.validate(obj);
        for ( ConstraintViolation<Object> violation : violations )
        {
            String path = getPath(violation);
            String message = String.format("%s - %s.%s = %s", violation.getMessage(), obj.getClass().getName(), path, String.valueOf(violation.getInvalidValue()));
            if ( exception == null )
            {
                exception = new ValidationException(message);
            }
            else
            {
                exception = new ValidationException(message, exception);
            }
        }
        return exception;
    }

    @SuppressWarnings("deprecation")
    private <T> void startInstance(T obj, Binding<T> binding, LifecycleMethods methods) throws Exception
    {
        log.debug(String.format("Starting %s", obj.getClass().getName()));

        setState(obj, LifecycleState.PRE_CONFIGURATION);
        for ( Method preConfiguration : methods.methodsFor(PreConfiguration.class) )
        {
            log.debug(String.format("\t%s()", preConfiguration.getName()));
            preConfiguration.invoke(obj);
        }

        setState(obj, LifecycleState.SETTING_CONFIGURATION);
        configurationMapper.mapConfiguration(configurationProvider, configurationDocumentation, obj, methods);

        setState(obj, LifecycleState.SETTING_RESOURCES);
        setResources(obj, methods);

        setState(obj, LifecycleState.POST_CONSTRUCTING);
        LinkedHashSet<Method> postConstructs = new LinkedHashSet<>();
        postConstructs.addAll(methods.methodsFor(PostConstruct.class));
        postConstructs.addAll(methods.methodsFor(WarmUp.class));
        for ( Method postConstruct : postConstructs )
        {
            log.debug(String.format("\t%s()", postConstruct.getName()));
            postConstruct.invoke(obj);
        }
        
        List<LifecycleAction> preDestroyActions = preDestroyLifecycleFeature.getActionsForType(obj.getClass());
        if ( !preDestroyActions.isEmpty() )
        {
            if (binding != null) {
                preDestroyMonitor.register(obj, binding, preDestroyActions);
            }
            else {
                preDestroyMonitor.register(obj, "legacy", preDestroyActions);
            }
        }

    }

    private void setResources(Object obj, LifecycleMethods methods) throws Exception
    {
        for ( Field field : methods.fieldsFor(Resources.class) )
        {
            Resources resources = field.getAnnotation(Resources.class);
            for ( Resource resource : resources.value() )
            {
                setFieldResource(obj, field, resource);
            }
        }

        for ( Field field : methods.fieldsFor(Resource.class) )
        {
            Resource resource = field.getAnnotation(Resource.class);
            setFieldResource(obj, field, resource);
        }

        for ( Method method : methods.methodsFor(Resources.class) )
        {
            Resources resources = method.getAnnotation(Resources.class);
            for ( Resource resource : resources.value() )
            {
                setMethodResource(obj, method, resource);
            }
        }

        for ( Method method : methods.methodsFor(Resource.class) )
        {
            Resource resource = method.getAnnotation(Resource.class);
            setMethodResource(obj, method, resource);
        }

        for ( Resources resources : methods.classAnnotationsFor(Resources.class) )
        {
            for ( Resource resource : resources.value() )
            {
                loadClassResource(resource);
            }
        }

        for ( Resource resource : methods.classAnnotationsFor(Resource.class) )
        {
            loadClassResource(resource);
        }
    }

    private void loadClassResource(Resource resource) throws Exception
    {
        if ( (resource.name().length() == 0) || (resource.type() == Object.class) )
        {
            throw new Exception("Class resources must have both name() and type(): " + resource);
        }
        findResource(resource);
    }

    private void setMethodResource(Object obj, Method method, Resource resource) throws Exception
    {
        if ( (method.getParameterTypes().length != 1) || (method.getReturnType() != Void.TYPE) )
        {
            throw new Exception(String.format("%s.%s() is not a proper JavaBean setter.", obj.getClass().getName(), method.getName()));
        }

        String beanName = method.getName();
        if ( beanName.toLowerCase().startsWith("set") )
        {
            beanName = beanName.substring("set".length());
        }
        beanName = Introspector.decapitalize(beanName);

        String siteName = obj.getClass().getName() + "/" + beanName;
        resource = adjustResource(resource, method.getParameterTypes()[0], siteName);
        Object resourceObj = findResource(resource);
        method.setAccessible(true);
        method.invoke(obj, resourceObj);
    }

    private void setFieldResource(Object obj, Field field, Resource resource) throws Exception
    {
        String siteName = obj.getClass().getName() + "/" + field.getName();
        Object resourceObj = findResource(adjustResource(resource, field.getType(), siteName));
        field.setAccessible(true);
        field.set(obj, resourceObj);
    }

    private Resource adjustResource(final Resource resource, final Class<?> siteType, final String siteName)
    {
        return new Resource()
        {
            @Override
            public String name()
            {
                return (resource.name().length() == 0) ? siteName : resource.name();
            }

            /**
             * Method needed for eventual java7 compatibility
             */
            public String lookup()
            {
                return name();
            }

            @Override
            public Class<?> type()
            {
                return (resource.type() == Object.class) ? siteType : resource.type();
            }

            @Override
            public AuthenticationType authenticationType()
            {
                return resource.authenticationType();
            }

            @Override
            public boolean shareable()
            {
                return resource.shareable();
            }

            @Override
            public String mappedName()
            {
                return resource.mappedName();
            }

            @Override
            public String description()
            {
                return resource.description();
            }

            @Override
            public Class<? extends Annotation> annotationType()
            {
                return resource.annotationType();
            }
        };
    }

    private Object findResource(Resource resource) throws Exception
    {
        if ( resourceLocators.size() > 0 )
        {
            final Iterator<ResourceLocator> iterator = resourceLocators.iterator();
            ResourceLocator locator = iterator.next();
            ResourceLocator nextInChain = new ResourceLocator()
            {
                @Override
                public Object locate(Resource resource, ResourceLocator nextInChain) throws Exception
                {
                    if ( iterator.hasNext() )
                    {
                        return iterator.next().locate(resource, this);
                    }
                    return defaultFindResource(resource);
                }
            };
            return locator.locate(resource, nextInChain);
        }
        return defaultFindResource(resource);
    }

    private Object defaultFindResource(Resource resource) throws Exception
    {
        if ( injector == null )
        {
            throw new NamingException("Could not find resource: " + resource);
        }

        //noinspection unchecked
        return injector.getInstance(resource.type());
    }

    private void stopInstances() throws Exception
    {
        preDestroyMonitor.close();
    }

    private String getPath(ConstraintViolation<Object> violation)
    {
        Iterable<String> transformed = Iterables.transform
            (
                violation.getPropertyPath(),
                new Function<Path.Node, String>()
                {
                    @Override
                    public String apply(Path.Node node)
                    {
                        return node.getName();
                    }
                }
            );
        return Joiner.on(".").join(transformed);
    }

    private void initializeObjectPostStart(Object obj) throws ValidationException
    {
        validate(obj);
    }

    private enum State
    {
        LATENT,
        STARTING,
        STARTED,
        CLOSED
    }

    @Override
    public void call(Injector injector) {
        preDestroyMonitor.addScopeBindings(injector.getScopeBindings());
    }   
}
