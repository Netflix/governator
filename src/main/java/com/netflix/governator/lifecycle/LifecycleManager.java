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
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.PreConfiguration;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.KeyParser;
import com.netflix.governator.lifecycle.warmup.DAGManager;
import com.netflix.governator.lifecycle.warmup.SetStateMixin;
import com.netflix.governator.lifecycle.warmup.WarmUpTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.xml.bind.DatatypeConverter;
import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import jsr166y.ForkJoinPool;
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
    private final ConfigurationDocumentation configurationDocumentation = new ConfigurationDocumentation();
    private final ConfigurationProvider configurationProvider;
    private final Collection<LifecycleListener> listeners;
    private final ValidatorFactory factory;
    private final DAGManager dagManager = new DAGManager();

    /**
     * Lifecycle managed objects have to be referenced via Object identity not equals()
     */
    private static class StateKey
    {
        final Object        obj;

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
        final Object                obj;
        final Collection<Method>    preDestroyMethods;

        private PreDestroyRecord(Object obj, Collection<Method> preDestroyMethods)
        {
            this.obj = obj;
            this.preDestroyMethods = preDestroyMethods;
        }
    }

    private enum State
    {
        LATENT,
        STARTING,
        STARTED,
        CLOSED
    }

    public LifecycleManager()
    {
        this(new LifecycleManagerArguments());
    }

    @Inject
    public LifecycleManager(LifecycleManagerArguments arguments)
    {
        configurationProvider = arguments.getConfigurationProvider();
        listeners = ImmutableSet.copyOf(arguments.getLifecycleListeners());
        factory = Validation.buildDefaultValidatorFactory();
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
     * @param obj object to add
     * @param methods calculated lifecycle methods
     * @throws Exception errors
     */
    public void add(Object obj, LifecycleMethods methods) throws Exception
    {
        Preconditions.checkState(state.get() != State.CLOSED, "LifecycleManager is closed");

        startInstance(obj, methods);

        if ( state.get() == State.STARTED )
        {
            initializeObjectPostStart(obj, methods);
        }
    }

    /**
     * Return the current state of the given object or LATENT if unknown
     *
     * @param obj object to check
     * @return state
     */
    public LifecycleState getState(Object obj)
    {
        if ( state.get() == State.STARTED )
        {
            return LifecycleState.ACTIVE;   // by definition
        }

        LifecycleState state = objectStates.get(new StateKey(obj));
        return (state != null) ? state : LifecycleState.LATENT;
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
     * @param unit time unit
     * @return true if warm up methods successfully executed, false if the time elapses
     * @throws Exception errors
     */
    public boolean start(long maxWait, TimeUnit unit) throws Exception
    {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTING), "Already started");

        validate();

        long        maxMs = (unit != null) ? unit.toMillis(maxWait) : Long.MAX_VALUE;
        boolean     success = doWarmUp(maxMs);

        configurationDocumentation.output(log);
        configurationDocumentation.clear();

        clear();

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
    public void        validate() throws ValidationException
    {
        ValidationException exception = null;
        Validator           validator = factory.getValidator();
        for ( StateKey key : objectStates.keySet() )
        {
            Object      obj = key.obj;
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
    public void        validate(Object obj) throws ValidationException
    {
        Validator           validator = factory.getValidator();
        ValidationException exception = internalValidateObject(null, obj, validator);
        if ( exception != null )
        {
            throw exception;
        }
    }

    public DAGManager getDAGManager()
    {
        return dagManager;
    }

    private void clear()
    {
        dagManager.clear();
        objectStates.clear();
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
            String      path = getPath(violation);
            String      message = String.format("%s - %s.%s = %s", violation.getMessage(), obj.getClass().getName(), path, String.valueOf(violation.getInvalidValue()));
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

    private boolean doWarmUp(long maxMs) throws Exception
    {
        for ( StateKey key : objectStates.keySet() )
        {
            objectStates.put(key, LifecycleState.PRE_WARMING_UP);
        }

        SetStateMixin       setState = new SetStateMixin()
        {
            @Override
            public void setState(Object obj, LifecycleState state)
            {
                LifecycleManager.this.setState(obj, state);
            }
        };
        ForkJoinPool                        forkJoinPool = new ForkJoinPool();
        ConcurrentMap<Object, WarmUpTask>   tasks = Maps.newConcurrentMap();
        WarmUpTask                          rootTask = new WarmUpTask(dagManager.buildTree(), this, setState, tasks, true);

        forkJoinPool.submit(rootTask);
        forkJoinPool.shutdown();

        boolean             success = forkJoinPool.awaitTermination(maxMs, TimeUnit.MILLISECONDS);
        if ( !success )
        {
            forkJoinPool.shutdownNow();
        }

        for ( StateKey key : objectStates.keySet() )
        {
            if ( objectStates.get(key) != LifecycleState.ERROR )
            {
                objectStates.put(key, LifecycleState.ACTIVE);
            }
        }

        return success;
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
        for ( Field configurationField : methods.fieldsFor(Configuration.class) )
        {
            assignConfiguration(obj, configurationField);
        }

        setState(obj, LifecycleState.POST_CONSTRUCTING);
        for ( Method postConstruct : methods.methodsFor(PostConstruct.class) )
        {
            log.debug(String.format("\t%s()", postConstruct.getName()));
            postConstruct.invoke(obj);
        }

        Collection<Method>      preDestroyMethods = methods.methodsFor(PreDestroy.class);
        if ( preDestroyMethods.size() > 0 )
        {
            preDestroys.add(new PreDestroyRecord(obj, preDestroyMethods));
        }
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

    private Date parseDate(String value)
    {
        DateFormat  formatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        formatter.setLenient(false);

        try
        {
            return formatter.parse(value);
        }
        catch( ParseException e )
        {
            // ignore
        }

        try
        {
            return DatatypeConverter.parseDateTime(value).getTime();
        }
        catch ( IllegalArgumentException e )
        {
            // ignore
        }

        return null;
    }

    private void assignConfiguration(Object obj, Field field) throws Exception
    {
        Configuration       configuration = field.getAnnotation(Configuration.class);
        String              configurationName = configuration.value();
        ConfigurationKey    key = new ConfigurationKey(configurationName, KeyParser.parse(configurationName));

        Object              value = null;

        boolean has = configurationProvider.has(key);
        if ( has )
        {
            if ( String.class.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getString(key);
            }
            else if ( Boolean.class.isAssignableFrom(field.getType()) || Boolean.TYPE.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getBoolean(key);
            }
            else if ( Integer.class.isAssignableFrom(field.getType()) || Integer.TYPE.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getInteger(key);
            }
            else if ( Long.class.isAssignableFrom(field.getType()) || Long.TYPE.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getLong(key);
            }
            else if ( Double.class.isAssignableFrom(field.getType()) || Double.TYPE.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getDouble(key);
            }
            else if ( Date.class.isAssignableFrom(field.getType()) )
            {
                value = parseDate(configurationProvider.getString(key));
            }
            else
            {
                log.error("Field type not supported: " + field.getType());
                field = null;
            }
        }

        if ( field != null )
        {
            String  defaultValue = String.valueOf(field.get(obj));
            String  documentationValue;
            if ( has )
            {
                field.set(obj, value);
                documentationValue = String.valueOf(value);
            }
            else
            {
                documentationValue = "";
            }
            configurationDocumentation.registerConfiguration(field, configurationName, has, defaultValue, documentationValue, configuration.documentation());
        }
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

    private void initializeObjectPostStart(Object obj, LifecycleMethods methods) throws ValidationException, IllegalAccessException, InvocationTargetException
    {
        validate(obj);

        objectStates.put(new StateKey(obj), LifecycleState.PRE_WARMING_UP);
        for ( Method method : methods.methodsFor(WarmUp.class) )
        {
            method.invoke(obj);
        }

        clear();
    }
}
