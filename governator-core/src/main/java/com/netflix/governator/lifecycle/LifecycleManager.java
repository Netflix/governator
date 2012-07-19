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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.governator.assets.AssetLoaderManager;
import com.netflix.governator.configuration.Configuration;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.SystemConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

// TODO - should the methods really be synchronized? Maybe just sync on the object being added

@Singleton
public class LifecycleManager implements Closeable
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<Object, LifecycleState> objectStates = Maps.newConcurrentMap();
    private final List<PreDestroyRecord> preDestroyRecords = new CopyOnWriteArrayList<PreDestroyRecord>();
    private final AtomicReference<State> state = new AtomicReference<State>(State.LATENT);
    private final AssetLoaderManager assetLoaderManager;
    private final ConfigurationProvider configurationProvider;

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
        STARTED,
        CLOSED
    }

    public LifecycleManager()
    {
        this.assetLoaderManager = new AssetLoaderManager();
        this.configurationProvider = new SystemConfigurationProvider();
    }

    @Inject
    public LifecycleManager(AssetLoaderManager assetLoaderManager, ConfigurationProvider configurationProvider)
    {
        this.assetLoaderManager = assetLoaderManager;
        this.configurationProvider = configurationProvider;
    }

    public AssetLoaderManager getAssetLoaderManager()
    {
        return assetLoaderManager;
    }

    public void add(Object... objects) throws Exception
    {
        for ( Object obj : objects )
        {
            add(obj);
        }
    }

    public synchronized void add(Object obj) throws Exception
    {
        add(obj, new LifecycleMethods(obj.getClass()));
    }

    public synchronized void add(Object obj, LifecycleMethods methods) throws Exception
    {
        Preconditions.checkState(state.get() != State.CLOSED, "LifecycleManager is closed");

        if ( getState(obj) == LifecycleState.LATENT )
        {
            objectStates.put(obj, LifecycleState.POST_CONSTRUCTING);
            try
            {
                startInstance(obj, methods);
            }
            catch ( Exception e )
            {
                objectStates.remove(obj);
                throw e;
            }
            objectStates.put(obj, LifecycleState.ACTIVE);
        }
    }

    public synchronized LifecycleState getState(Object obj)
    {
        LifecycleState state = objectStates.get(obj);
        return (state != null) ? state : LifecycleState.LATENT;
    }

    public void start() throws Exception
    {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTED), "Already started");

        // currently a NOP
    }

    @Override
    public synchronized void close() throws IOException
    {
        if ( state.compareAndSet(State.STARTED, State.CLOSED) )
        {
            try
            {
                stopInstances();
            }
            finally
            {
                preDestroyRecords.clear();
                objectStates.clear();
            }
        }
    }

    private void startInstance(Object obj, LifecycleMethods methods) throws Exception
    {
        log.debug("Starting %s", obj.getClass().getName());

        boolean             hasAssets = assetLoaderManager.loadAssetsFor(obj);

        for ( Field configurationField : methods.fieldsFor(Configuration.class) )
        {
            assignConfiguration(obj, configurationField);
        }

        for ( Method postConstruct : methods.methodsFor(PostConstruct.class) )
        {
            log.debug("\t%s()", postConstruct.getName());
            postConstruct.invoke(obj);
        }

        Collection<Method>      preDestroyMethods = methods.methodsFor(PreDestroy.class);
        if ( hasAssets || (preDestroyMethods.size() > 0) )
        {
            preDestroyRecords.add(new PreDestroyRecord(obj, preDestroyMethods));
        }
    }

    private void stopInstances()
    {
        List<PreDestroyRecord> reversed = Lists.newArrayList(preDestroyRecords);
        Collections.reverse(reversed);

        for ( PreDestroyRecord record : reversed )
        {
            objectStates.put(record.obj, LifecycleState.PRE_DESTROYING);

            try
            {
                assetLoaderManager.unloadAssetsFor(record.obj);
            }
            catch ( Throwable e )
            {
                log.error("Couldn't unload assets lifecycle managed instance of type: " + record.obj.getClass().getName(), e);
            }

            log.debug("Stopping %s", record.obj.getClass().getName());
            for ( Method preDestroy : record.preDestroyMethods )
            {
                log.debug("\t%s()", preDestroy.getName());
                try
                {
                    preDestroy.invoke(record.obj);
                }
                catch ( Throwable e )
                {
                    log.error("Couldn't stop lifecycle managed instance", e);
                }
            }
        }
    }

    private void assignConfiguration(Object obj, Field field) throws Exception
    {
        Configuration       configuration = field.getAnnotation(Configuration.class);
        if ( configurationProvider.has(configuration.value()) )
        {
            if ( field.getType() == String.class )
            {
                String  value = configurationProvider.getString(configuration.value());
                log.debug("String %s = \"%s\"", field.getName(), value);
                field.set(obj, value);
            }
            else if ( (field.getType() == Boolean.class) || (field.getType() == Boolean.TYPE) )
            {
                boolean  value = configurationProvider.getBoolean(configuration.value());
                log.debug("boolean %s = %s", field.getName(), Boolean.toString(value));
                field.set(obj, value);
            }
            else if ( (field.getType() == Integer.class) || (field.getType() == Integer.TYPE) )
            {
                int  value = configurationProvider.getInteger(configuration.value());
                log.debug("int %s = %d", field.getName(), value);
                field.set(obj, value);
            }
            else if ( (field.getType() == Long.class) || (field.getType() == Long.TYPE) )
            {
                long  value = configurationProvider.getLong(configuration.value());
                log.debug("int %s = %d", field.getName(), value);
                field.set(obj, value);
            }
            else if ( (field.getType() == Double.class) || (field.getType() == Double.TYPE) )
            {
                double  value = configurationProvider.getDouble(configuration.value());
                log.debug("int %s = %f", field.getName(), value);
                field.set(obj, value);
            }
            else
            {
                log.error("Field type not supported: " + field.getType());
            }
        }
    }
}
