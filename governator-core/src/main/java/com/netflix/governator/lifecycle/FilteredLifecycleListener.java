/*
 * Copyright 2013 Netflix, Inc.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.TypeLiteral;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper listener that forwards to the provided listener only when the obj is in one of the
 * specified base packages.
 */
public class FilteredLifecycleListener extends DefaultLifecycleListener
{
    private final ImmutableSet<String> packages;
    private final LifecycleListener listener;

    /**
     * @param listener actual listener
     * @param basePackages set of base packages
     */
    public FilteredLifecycleListener(LifecycleListener listener, String... basePackages)
    {
        this(listener, Sets.newHashSet(Arrays.asList(basePackages)));
    }

    /**
     * @param listener actual listener
     * @param basePackages set of base packages
     */
    public FilteredLifecycleListener(LifecycleListener listener, Collection<String> basePackages)
    {
        this.listener = listener;
        packages = ImmutableSet.copyOf(basePackages);
    }

    @Override
    public <T> void objectInjected(TypeLiteral<T> type, T obj)
    {
        if ( isInPackages(obj) )
        {
            listener.objectInjected(type, obj);
        }
    }

    @Override
    public void stateChanged(Object obj, LifecycleState newState)
    {
        if ( isInPackages(obj) )
        {
            listener.stateChanged(obj, newState);
        }
    }

    private boolean isInPackages(Object obj)
    {
        if ( obj != null )
        {
            return isInPackages(obj.getClass());
        }
        return false;
    }
    
    private boolean isInPackages(Class type)
    {
        if ( type != null )
        {
            for ( String p : packages )
            {
                if ( type.getPackage().getName().startsWith(p) )
                {
                    return true;
                }
            }
        }
        return false;
    }

//    @Override
//    public <T> void objectInjected(TypeLiteral<T> type, T obj, long duration, TimeUnit units) {
//        if ( isInPackages(obj) )
//        {
//            listener.objectInjected(type, obj, duration, units);
//        }
//    }
//
//    @Override
//    public <T> void objectInjecting(TypeLiteral<T> type) {
//        if ( isInPackages(type.getRawType()) )
//        {
//            listener.objectInjecting(type);
//        }
//    }
}
