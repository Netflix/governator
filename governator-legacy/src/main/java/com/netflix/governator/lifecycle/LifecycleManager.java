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

import static com.netflix.governator.internal.BinaryConstant.I10_1024;
import static com.netflix.governator.internal.BinaryConstant.I15_32768;
import static com.netflix.governator.internal.BinaryConstant.I16_65536;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
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
    private enum State
    {
        LATENT,
        STARTING,
        STARTED,
        CLOSED
    }

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<Object, LifecycleStateWrapper> objectStates = new MapMaker().weakKeys().initialCapacity(I16_65536).concurrencyLevel(I10_1024).makeMap();
    private final PreDestroyLifecycleFeature preDestroyLifecycleFeature = new PreDestroyLifecycleFeature(ValidationMode.LAX);
    private final ConcurrentMap<Class<?>, List<LifecycleAction>> preDestroyActionCache = new ConcurrentHashMap<Class<?>, List<LifecycleAction>>(I15_32768);

    private final AtomicReference<State> state = new AtomicReference<State>(State.LATENT);
    private final ConfigurationDocumentation configurationDocumentation;
    private final ConfigurationProvider configurationProvider;
    private final ConfigurationMapper configurationMapper;
    private final ResourceMapper resourceMapper;
    final LifecycleListener[] listeners;
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
        if (injector != null) {
            preDestroyMonitor =  new PreDestroyMonitor(injector.getScopeBindings());
        }
        else {
            preDestroyMonitor = null;
        }
        configurationMapper = arguments.getConfigurationMapper();
        newLifecycleManager = arguments.getLifecycleManager();
        listeners = arguments.getLifecycleListeners().toArray(new LifecycleListener[0]);
        resourceMapper = new ResourceMapper(injector, ImmutableSet.copyOf(arguments.getResourceLocators()));
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
        return Arrays.asList(listeners);
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
        add(obj, null, methods);
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
       State managerState = state.get();
       if (managerState != State.CLOSED) {     
           startInstance(obj, binding, methods);
           if ( managerState == State.STARTED )
           {
               initializeObjectPostStart(obj);
           }
       }
       else {
           throw new IllegalStateException("LifecycleManager is closed");
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
        LifecycleStateWrapper lifecycleState = objectStates.get(obj);
        if ( lifecycleState == null )
        {
            return hasStarted() ? LifecycleState.ACTIVE : LifecycleState.LATENT;
        }
        else {
            synchronized(lifecycleState) {
                return lifecycleState.get();
            }
        }
    }

    /**
     * The manager MUST be started. Note: this method
     * waits indefinitely for warm up methods to complete
     *
     * @throws Exception errors
     */
    public void start() throws Exception
    {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTING), "Already started");

        new ConfigurationColumnWriter(configurationDocumentation).output(log);
        if (newLifecycleManager != null) {
            newLifecycleManager.notifyStarted();
        }
        state.set(State.STARTED);    
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
        start();
        return true;
    }

    @SuppressWarnings("deprecation")
    private <T> void startInstance(T obj, Binding<T> binding, LifecycleMethods methods) throws Exception
    {
        final Class<?> instanceType = obj.getClass();
        log.debug("Starting {}", instanceType.getName());

        final LifecycleStateWrapper lifecycleState = initState(obj, LifecycleState.PRE_CONFIGURATION);
        methods.methodInvoke(PreConfiguration.class, obj);

        lifecycleState.set(obj, LifecycleState.SETTING_CONFIGURATION);
        configurationMapper.mapConfiguration(configurationProvider, configurationDocumentation, obj, methods);

        lifecycleState.set(obj, LifecycleState.SETTING_RESOURCES);
        resourceMapper.map(obj, methods);

        lifecycleState.set(obj, LifecycleState.POST_CONSTRUCTING);
        methods.methodInvoke(PostConstruct.class, obj);
        
        Method[] warmUpMethods = methods.annotatedMethods(WarmUp.class);
        if (warmUpMethods.length > 0) {
            Method[] postConstructMethods = methods.annotatedMethods(PostConstruct.class);
            for ( Method warmupMethod : warmUpMethods)
            {
                boolean skipWarmup = false;
                // assuming very few methods in both WarmUp and PostConstruct
                for (Method postConstruct :  postConstructMethods) {
                    if (postConstruct == warmupMethod) {
                        skipWarmup = true;
                        break;
                    }
                }
                if (!skipWarmup) {
                    log.debug("\t{}()", warmupMethod.getName());
                    LifecycleMethods.methodInvoke(warmupMethod, obj);
                }
            }
        }

        List<LifecycleAction> preDestroyActions;
        if (preDestroyActionCache.containsKey(instanceType)) {
            preDestroyActions = preDestroyActionCache.get(instanceType);
        }
        else {
            preDestroyActions = preDestroyLifecycleFeature.getActionsForType(instanceType);
            preDestroyActionCache.put(instanceType, preDestroyActions);
        }
        
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

    class LifecycleStateWrapper {
        LifecycleState state;

        public void set(Object managedInstance, LifecycleState state) {
            this.state = state;
            for ( LifecycleListener listener : listeners )
            {
                listener.stateChanged(managedInstance, state);
            }            
        }
        
        public LifecycleState get() {
            return state;
        }
    }

    private LifecycleStateWrapper initState(Object obj, LifecycleState state) {
        LifecycleStateWrapper stateWrapper = new LifecycleStateWrapper();
        objectStates.put(obj, stateWrapper);
        stateWrapper.set(obj, state);
        return stateWrapper;
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
                preDestroyMonitor.close();
            }
            catch ( Exception e )
            {
                log.error("While stopping instances", e);
            }
            finally
            {
                objectStates.clear();
                preDestroyActionCache.clear();
            }
        }
    }

    private void initializeObjectPostStart(Object obj)
    {
        
    }

    
    @Override
    public void call(Injector injector) {
        this.resourceMapper.setInjector(injector);
        this.preDestroyMonitor.addScopeBindings(injector.getScopeBindings());
    }
}
