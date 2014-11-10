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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.PreConfiguration;
import com.netflix.governator.configuration.ConfigurationColumnWriter;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationMapper;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.warmup.DAGManager;
import com.netflix.governator.lifecycle.warmup.WarmUpDriver;
import com.netflix.governator.lifecycle.warmup.WarmUpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.naming.NamingException;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.beans.Introspector;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main instance management container
 */
@Singleton
public class LifecycleManager implements Closeable
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<StateKey, LifecycleState> objectStates = Maps.newConcurrentMap();
    private final List<PreDestroyRecord> preDestroys = new CopyOnWriteArrayList<PreDestroyRecord>();
    private final AtomicReference<State> state = new AtomicReference<State>(State.LATENT);
    private final ConfigurationDocumentation configurationDocumentation;
    private final ConfigurationProvider configurationProvider;
    private final ConfigurationMapper configurationMapper;
    private final Collection<LifecycleListener> listeners;
    private final Collection<ResourceLocator> resourceLocators;
    private final ValidatorFactory factory;
    private final DAGManager dagManager = new DAGManager();
    private final PostStartArguments postStartArguments;
    private final AtomicReference<WarmUpSession> postStartWarmUpSession = new AtomicReference<WarmUpSession>(null);
    private final Injector injector;

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
        configurationMapper = arguments.getConfigurationMapper();
        listeners = ImmutableSet.copyOf(arguments.getLifecycleListeners());
        resourceLocators = ImmutableSet.copyOf(arguments.getResourceLocators());
        factory = Validation.buildDefaultValidatorFactory();
        postStartArguments = arguments.getPostStartArguments();
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
    public void add(Object obj) throws Exception
    {
        add(obj, new LifecycleMethods(obj.getClass()));
    }

    /**
     * Add the object to the container. Its assets will be loaded, post construct methods called, etc.
     * This version helps performance when the lifecycle methods have already been calculated
     *
     * @param obj     object to add
     * @param methods calculated lifecycle methods
     * @throws Exception errors
     */
    public void add(Object obj, LifecycleMethods methods) throws Exception
    {
        Preconditions.checkState(state.get() != State.CLOSED, "LifecycleManager is closed");

        startInstance(obj, methods);

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
        LifecycleState lifecycleState = objectStates.get(new StateKey(obj));
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
    public boolean start(long maxWait, TimeUnit unit) throws Exception
    {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTING), "Already started");

        validate();

        long maxMs = (unit != null) ? unit.toMillis(maxWait) : Long.MAX_VALUE;
        WarmUpSession warmUpSession = new WarmUpSession(getWarmUpDriver(), dagManager);
        boolean success = warmUpSession.doImmediate(maxMs);

        new ConfigurationColumnWriter(configurationDocumentation).output(log);

        state.set(State.STARTED);

        return success;
    }

    @Override
    public synchronized void close()
    {
        if ( state.compareAndSet(State.STARTING, State.CLOSED) || state.compareAndSet(State.STARTED, State.CLOSED) )
        {
            try
            {
                stopInstances();
            }
            catch ( Exception e )
            {
                log.error("While stopping instances", e);
            }
            finally
            {
                preDestroys.clear();
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
        for ( StateKey key : objectStates.keySet() )
        {
            Object obj = key.obj;
            exception = internalValidateObject(exception, obj, validator);
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

    /**
     * @return the internal DAG manager
     */
    public DAGManager getDAGManager()
    {
        return dagManager;
    }

    private void setState(Object obj, LifecycleState state)
    {
        objectStates.put(new StateKey(obj), state);
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

    private void startInstance(Object obj, LifecycleMethods methods) throws Exception
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
        for ( Method postConstruct : methods.methodsFor(PostConstruct.class) )
        {
            log.debug(String.format("\t%s()", postConstruct.getName()));
            postConstruct.invoke(obj);
        }

        Collection<Method> preDestroyMethods = methods.methodsFor(PreDestroy.class);
        if ( preDestroyMethods.size() > 0 )
        {
            preDestroys.add(new PreDestroyRecord(obj, preDestroyMethods));
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

    private Resource adjustResource(final Resource resource, final Class siteType, final String siteName)
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
            public Class type()
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
        for ( PreDestroyRecord record : getReversed(preDestroys) )
        {
            log.debug(String.format("Stopping %s:%d", record.obj.getClass().getName(), System.identityHashCode(record.obj)));
            setState(record.obj, LifecycleState.PRE_DESTROYING);

            for ( Method preDestroy : record.preDestroyMethods )
            {
                log.debug(String.format("\t%s()", preDestroy.getName()));
                try
                {
                    preDestroy.invoke(record.obj);
                }
                catch ( Throwable e )
                {
                    log.error("Couldn't stop lifecycle managed instance", e);
                }
            }

            objectStates.remove(new StateKey(record.obj));
        }
    }

    private List<PreDestroyRecord> getReversed(List<PreDestroyRecord> records)
    {
        List<PreDestroyRecord> reversed = Lists.newArrayList(records);
        Collections.reverse(reversed);
        return reversed;
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

        postStartWarmUpSession.compareAndSet(null, new WarmUpSession(getWarmUpDriver(), dagManager));
        WarmUpSession session = postStartWarmUpSession.get();
        session.doInBackground();
    }

    private WarmUpDriver getWarmUpDriver()
    {
        return new WarmUpDriver()
        {
            @Override
            public void setPreWarmUpState()
            {
                for ( StateKey key : objectStates.keySet() )
                {
                    objectStates.put(key, LifecycleState.PRE_WARMING_UP);
                }
            }

            @Override
            public void setPostWarmUpState()
            {
                Iterator<LifecycleState> iterator = objectStates.values().iterator();
                while ( iterator.hasNext() )
                {
                    if ( iterator.next() != LifecycleState.ERROR )
                    {
                        iterator.remove();
                    }
                }
            }

            @Override
            public PostStartArguments getPostStartArguments()
            {
                return postStartArguments;
            }

            @Override
            public void setState(Object obj, LifecycleState state)
            {
                LifecycleManager.this.setState(obj, state);
            }
        };
    }

    private enum State
    {
        LATENT,
        STARTING,
        STARTED,
        CLOSED
    }

    /**
     * Lifecycle managed objects have to be referenced via Object identity not equals()
     */
    private static class StateKey
    {
        final Object obj;

        private StateKey(Object obj)
        {
            this.obj = obj;
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(obj);
        }

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object o)
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            return hashCode() == o.hashCode();
        }
    }

    private static class PreDestroyRecord
    {
        final Object obj;
        final Collection<Method> preDestroyMethods;

        private PreDestroyRecord(Object obj, Collection<Method> preDestroyMethods)
        {
            this.obj = obj;
            this.preDestroyMethods = preDestroyMethods;
        }
    }
}
