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

package com.netflix.governator.guice;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.LifecycleMethodsFactory;
import com.netflix.governator.lifecycle.warmup.DAGManager;

class InternalLifecycleModule extends AbstractModule {

    public static class LifecycleInjectionListener implements TypeListener {

        // this really serves as a Set purpose.
        // put dummy boolean as Map value.
        // value is really not important here.
        private final CopyOnWriteArraySet<Dependency<?>> seen = new CopyOnWriteArraySet<Dependency<?>>();

        private final ConcurrentMap<Class<?>, LifecycleMethods> lifecycleMethods = Maps.newConcurrentMap();
        
        private final LifecycleManager manager;
        
        private final LifecycleMethodsFactory factory;
        
        LifecycleInjectionListener(LifecycleManager manager, LifecycleMethodsFactory factory) {
            this.manager = manager;
            this.factory = factory;
        }
        
        @Override
        public <I> void hear(final TypeLiteral<I> type, TypeEncounter<I> encounter) {
            encounter.register(
                    new InjectionListener<I>() {
                        @Override
                        public void afterInjection(I obj) {
                            processInjectedObject(obj, type);
                        }
                    }
                );
        }
        
        private <T> void processInjectedObject(T obj, TypeLiteral<T> type) {
            for ( LifecycleListener listener : manager.getListeners() ) {
                listener.objectInjected(type, obj);
            }

            Class<?> clazz = obj.getClass();
            LifecycleMethods methods = getLifecycleMethods(clazz);

            if ( warmUpIsInDag(clazz, type) ) {
                addDependencies(manager, obj, type, methods);
            }

            if ( methods.hasLifecycleAnnotations() ) {
                try {
                    manager.add(obj, methods);
                }
                catch ( Exception e ) {
                    throw new Error(e);
                }
            }
        }

        private void addDependencies(LifecycleManager manager, Object obj, TypeLiteral<?> type, LifecycleMethods methods) {
            DAGManager dagManager = manager.getDAGManager();
            dagManager.addObjectMapping(type, obj, methods);

            applyInjectionPoint(getConstructorInjectionPoint(type), dagManager, type);
            for ( InjectionPoint injectionPoint : getMethodInjectionPoints(type) )
            {
                applyInjectionPoint(injectionPoint, dagManager, type);
            }
        }

        private boolean warmUpIsInDag(Class<?> clazz, TypeLiteral<?> type) {
            LifecycleMethods methods = getLifecycleMethods(clazz);
            if ( methods.methodsFor(WarmUp.class).size() > 0 ) {
                return true;
            }

            if ( warmUpIsInDag(getConstructorInjectionPoint(type)) ) {
                return true;
            }

            for ( InjectionPoint injectionPoint : getMethodInjectionPoints(type) ) {
                if ( warmUpIsInDag(injectionPoint) ) {
                    return true;
                }
            }

            return false;
        }

        private boolean warmUpIsInDag(InjectionPoint injectionPoint)
        {
            if ( injectionPoint == null ) {
                return false;
            }

            List<Dependency<?>> dependencies = injectionPoint.getDependencies();
            for ( Dependency<?> dependency : dependencies ) {
                if (seen.add(dependency)) {
                    if ( warmUpIsInDag(dependency.getKey().getTypeLiteral().getRawType(), dependency.getKey().getTypeLiteral()) ) {
                        return true;
                    }
                }
            }
            return false;
        }

        private Set<InjectionPoint> getMethodInjectionPoints(TypeLiteral<?> type) {
            try {
                return InjectionPoint.forInstanceMethodsAndFields(type);
            }
            catch ( NullPointerException e ) {
                // ignore - unfortunately this is happening inside of Guice
            }
            catch ( ConfigurationException e ) {
                // ignore
            }
            return Sets.newHashSet();
        }

        private InjectionPoint getConstructorInjectionPoint(TypeLiteral<?> type) {
            try {
                return InjectionPoint.forConstructorOf(type);
            }
            catch ( ConfigurationException e ) {
                // ignore
            }
            return null;
        }

        private void applyInjectionPoint(InjectionPoint injectionPoint, DAGManager dagManager, TypeLiteral<?> type) {
            if ( injectionPoint != null ) {
                for ( Dependency<?> dependency : injectionPoint.getDependencies() ) {
                    dagManager.addDependency(type, dependency.getKey().getTypeLiteral());
                }
            }
        }

        private LifecycleMethods getLifecycleMethods(Class<?> clazz) {
            LifecycleMethods methods = lifecycleMethods.get(clazz);
            if (methods == null) {
                methods = factory.create(clazz);
                LifecycleMethods existing = lifecycleMethods.putIfAbsent(clazz, methods);
                if (existing != null) {
                    return existing;
                }
            }
            return methods;
        }
    }
    
    final private LifecycleMethodsFactory factory;
    final private LifecycleManager manager;
    
    public InternalLifecycleModule(LifecycleManager manager, LifecycleMethodsFactory factory) {
        this.factory = factory;
        this.manager = manager;
    }
    
    @Override
    public void configure() {
        bindListener(Matchers.any(), new LifecycleInjectionListener(manager, factory));
    }
}