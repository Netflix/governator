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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.CoolDown;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.assets.AssetLoading;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationProvider;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

// TODO - should the methods really be synchronized? Maybe just sync on the object being added

/**
 * Main instance management container
 */
public class LifecycleManager implements Closeable
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<StateKey, LifecycleState> objectStates = Maps.newConcurrentMap();
    private final List<InvokeRecord> invokings = new CopyOnWriteArrayList<InvokeRecord>();
    private final AtomicReference<State> state = new AtomicReference<State>(State.LATENT);
    private final ConfigurationDocumentation configurationDocumentation = new ConfigurationDocumentation();
    private final AssetLoading assetLoading;
    private final ConfigurationProvider configurationProvider;

    private volatile long maxCoolDownMs = TimeUnit.MINUTES.toMillis(1);
    private volatile LifecycleListener listener = null;

    public static final String      DEFAULT_ASSET_LOADER_VALUE = "";

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

    private static class InvokeRecord
    {
        final Object                obj;
        final Collection<Method>    preDestroyMethods;
        final Collection<Method>    warmUpMethods;
        final Collection<Method>    coolDownMethods;
        final boolean               hasAssets;

        private InvokeRecord(Object obj, Collection<Method> preDestroyMethods, Collection<Method> warmUpMethods, Collection<Method> coolDownMethods, boolean hasAssets)
        {
            this.obj = obj;
            this.preDestroyMethods = preDestroyMethods;
            this.warmUpMethods = warmUpMethods;
            this.coolDownMethods = coolDownMethods;
            this.hasAssets = hasAssets;
        }
    }

    private enum State
    {
        LATENT,
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
        assetLoading = new AssetLoading(arguments.assetLoaders);
        configurationProvider = arguments.configurationProvider;
    }

    /**
     * Set the lifecycle listener
     *
     * @param listener the listener
     */
    public void setListener(LifecycleListener listener)
    {
        this.listener = listener;
    }

    /**
     * Set the maximum time to wait for cool downs to complete. The default is 1 minute
     *
     * @param maxCoolDownMs max cool down in milliseconds
     */
    public void setMaxCoolDownMs(long maxCoolDownMs)
    {
        this.maxCoolDownMs = maxCoolDownMs;
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
    public synchronized void add(Object obj) throws Exception
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
    public synchronized void add(Object obj, LifecycleMethods methods) throws Exception
    {
        Preconditions.checkState(state.get() != State.CLOSED, "LifecycleManager is closed");

        StateKey        key = new StateKey(obj);
        if ( getState(key) == LifecycleState.LATENT )
        {
            try
            {
                startInstance(obj, methods);
            }
            catch ( Exception e )
            {
                objectStates.remove(key);
                throw e;
            }
            objectStates.put(key, LifecycleState.ACTIVE);
        }
        else
        {
            log.warn(String.format("Object already completed lifecycle. class: %s - ID: %d", obj.getClass().getName(), System.identityHashCode(obj)));
        }
    }

    /**
     * Return the current state of the given object or LATENT if unknown
     *
     * @param obj object to check
     * @return state
     */
    public synchronized LifecycleState getState(Object obj)
    {
        LifecycleState state = objectStates.get(new StateKey(obj));
        return (state != null) ? state : LifecycleState.LATENT;
    }

    /**
     * The manager MUST be started
     *
     * @throws Exception errors
     */
    public void start() throws Exception
    {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTED), "Already started");

        validate();

        doWarmUp();

        configurationDocumentation.output(log);
        configurationDocumentation.clear();
    }

    @Override
    public synchronized void close()
    {
        if ( state.compareAndSet(State.STARTED, State.CLOSED) )
        {
            try
            {
                doCoolDown();
            }
            catch ( Exception e )
            {
                log.error("While cooling down instances", e);
            }

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
                invokings.clear();
                objectStates.clear();
            }
        }
    }

    void setState(Object obj, LifecycleState state)
    {
        objectStates.put(new StateKey(obj), state);
        if ( listener != null )
        {
            listener.stateChanged(obj, state);
        }
    }

    @VisibleForTesting
    protected int getWarmUpThreadQty()
    {
        return Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    }

    private void doCoolDown() throws Exception
    {
        WarmUpManager       manager = new WarmUpManager(this, LifecycleState.PRE_DESTROYING, getWarmUpThreadQty());

        for ( InvokeRecord record : getReversed(invokings) )
        {
            if ( record.coolDownMethods.size() > 0 )
            {
                setState(record.obj, LifecycleState.COOLING_DOWN);
                log.debug(String.format("Cooling down %s:%d", record.obj.getClass().getName(), System.identityHashCode(record.obj)));

                for ( Method m : record.coolDownMethods )
                {
                    manager.add(record.obj, m);
                }
            }
        }

        if ( !manager.runAllAndWait(maxCoolDownMs, TimeUnit.MILLISECONDS) )
        {
            log.error("Some cool down methods did not complete before the timeout of " + maxCoolDownMs + " ms");
        }
    }

    private void doWarmUp() throws Exception
    {
        for ( StateKey key : objectStates.keySet() )
        {
            objectStates.put(key, LifecycleState.ACTIVE);
        }

        WarmUpManager       manager = new WarmUpManager(this, LifecycleState.ACTIVE, getWarmUpThreadQty());

        for ( InvokeRecord record : invokings )
        {
            if ( record.warmUpMethods.size() > 0 )
            {
                setState(record.obj, LifecycleState.WARMING_UP);
                log.debug(String.format("Warming up %s:%d", record.obj.getClass().getName(), System.identityHashCode(record.obj)));

                for ( Method m : record.warmUpMethods )
                {
                    manager.add(record.obj, m);
                }
            }
        }

        manager.runAll();
    }

    private void startInstance(Object obj, LifecycleMethods methods) throws Exception
    {
        log.debug(String.format("Starting %s", obj.getClass().getName()));

        setState(obj, LifecycleState.LOADING_ASSETS);

        boolean             hasAssets = assetLoading.loadAssetsFor(obj);

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
        Collection<Method>      warmUpMethods = methods.methodsFor(WarmUp.class);
        Collection<Method>      coolDownMethods = methods.methodsFor(CoolDown.class);
        if ( hasAssets || (preDestroyMethods.size() > 0) || (warmUpMethods.size() > 0) || (coolDownMethods.size() > 0) )
        {
            invokings.add(new InvokeRecord(obj, preDestroyMethods, warmUpMethods, coolDownMethods, hasAssets));
        }
    }

    private void stopInstances() throws Exception
    {
        for ( InvokeRecord record : getReversed(invokings) )
        {
            if ( record.hasAssets )
            {
                try
                {
                    assetLoading.unloadAssetsFor(record.obj);
                }
                catch ( Throwable e )
                {
                    log.error("Couldn't unload assets lifecycle managed instance of type: " + record.obj.getClass().getName(), e);
                }
            }

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

    private List<InvokeRecord> getReversed(List<InvokeRecord> records)
    {
        List<InvokeRecord> reversed = Lists.newArrayList(records);
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

        Object      value = null;

        boolean has = configurationProvider.has(configurationName);
        if ( has )
        {
            if ( String.class.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getString(configurationName);
            }
            else if ( Boolean.class.isAssignableFrom(field.getType()) || Boolean.TYPE.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getBoolean(configurationName);
            }
            else if ( Integer.class.isAssignableFrom(field.getType()) || Integer.TYPE.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getInteger(configurationName);
            }
            else if ( Long.class.isAssignableFrom(field.getType()) || Long.TYPE.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getLong(configurationName);
            }
            else if ( Double.class.isAssignableFrom(field.getType()) || Double.TYPE.isAssignableFrom(field.getType()) )
            {
                value = configurationProvider.getDouble(configurationName);
            }
            else if ( Date.class.isAssignableFrom(field.getType()) )
            {
                value = parseDate(configurationProvider.getString(configurationName));
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

    private void        validate() throws ValidationException
    {
        ValidationException exception = null;
        ValidatorFactory    factory = Validation.buildDefaultValidatorFactory();
        Validator           validator = factory.getValidator();
        for ( StateKey key : objectStates.keySet() )
        {
            Set<ConstraintViolation<Object>> violations = validator.validate(key.obj);
            for ( ConstraintViolation<Object> violation : violations )
            {
                String      path = getPath(violation);
                String      message = String.format("%s - %s.%s = %s", violation.getMessage(), key.obj.getClass().getName(), path, String.valueOf(violation.getInvalidValue()));
                if ( exception == null )
                {
                    exception = new ValidationException(message);
                }
                else
                {
                    exception = new ValidationException(message, exception);
                }
            }
        }

        if ( exception != null )
        {
            throw exception;
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
}
