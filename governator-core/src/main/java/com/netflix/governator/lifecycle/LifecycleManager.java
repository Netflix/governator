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

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.configuration.ConfigurationColumnWriter;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.guice.LifecycleAnnotationProcessor;
import com.netflix.governator.lifecycle.warmup.DAGManager;
import com.netflix.governator.lifecycle.warmup.WarmUpDriver;
import com.netflix.governator.lifecycle.warmup.WarmUpSession;

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
    private final Collection<LifecycleListener> listeners;
    private final ValidatorFactory factory;
    private final DAGManager dagManager = new DAGManager();
    private final PostStartArguments postStartArguments;
    private final AtomicReference<WarmUpSession> postStartWarmUpSession = new AtomicReference<WarmUpSession>(null);
    private final LifecycleMethodsFactory methodsFactory;
    
    private final List<List<LifecycleAnnotationProcessor>> processors = Lists.newArrayList();
    
    @Inject
    public LifecycleManager(LifecycleManagerArguments arguments, LifecycleMethodsFactory methodsFactory)
    {
        this.methodsFactory = methodsFactory;
        listeners = ImmutableSet.copyOf(arguments.getLifecycleListeners());
        factory = Validation.buildDefaultValidatorFactory();
        postStartArguments = arguments.getPostStartArguments();
        configurationDocumentation = arguments.getConfigurationDocumentation();
        
        for (LifecycleState state : LifecycleState.values()) {
            processors.add(Lists.<LifecycleAnnotationProcessor>newArrayList());
        }
        
        for (LifecycleAnnotationProcessor processor : arguments.getAnnotationProcessors()) {
            processors.get(processor.getState().ordinal()).add(processor);
        }
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
        add(obj, methodsFactory.create(obj.getClass()));
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

        log.debug(String.format("Starting %s", obj.getClass().getName()));

        setState(obj, methods, LifecycleState.PRE_CONFIGURATION);
        setState(obj, methods, LifecycleState.SETTING_CONFIGURATION);
        setState(obj, methods, LifecycleState.SETTING_RESOURCES);
        setState(obj, methods, LifecycleState.POST_CONSTRUCTING);
        
        if ( !methods.methodsFor(PreDestroy.class).isEmpty() )
        {
            preDestroys.add(new PreDestroyRecord(obj, methods));
        }

        if ( hasStarted() )
        {
            validate(obj);

            postStartWarmUpSession.compareAndSet(null, new WarmUpSession(getWarmUpDriver(), dagManager));
            WarmUpSession session = postStartWarmUpSession.get();
            session.doInBackground();
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

    private void setState(Object obj, LifecycleMethods methods, LifecycleState state) throws Exception
    {
        objectStates.put(new StateKey(obj), state);
        for ( LifecycleListener listener : listeners )
        {
            listener.stateChanged(obj, state);
        }
        
        if (methods != null) {
            for (LifecycleAnnotationProcessor processor : processors.get(state.ordinal())) {
                processor.process(obj, methods);
            }
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

    private void stopInstances() throws Exception
    {
        for ( PreDestroyRecord record : getReversed(preDestroys) )
        {
            log.debug(String.format("Stopping %s:%d", record.obj.getClass().getName(), System.identityHashCode(record.obj)));
            setState(record.obj, record.methods, LifecycleState.PRE_DESTROYING);
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
                try {
                    LifecycleManager.this.setState(obj, null, state);
                } catch (Exception e) {
                    log.warn("Error changing state in warmp phase", e);
                }
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

            return hashCode() == ((StateKey)o).hashCode();
        }
    }

    private static class PreDestroyRecord
    {
        final Object obj;
        final LifecycleMethods methods;

        private PreDestroyRecord(Object obj, LifecycleMethods methods)
        {
            this.obj = obj;
            this.methods = methods;
        }
    }
}
