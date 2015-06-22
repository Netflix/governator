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

import java.util.concurrent.TimeUnit;

import com.google.inject.TypeLiteral;

/**
 * Callback for injected instances
 */
public interface LifecycleListener
{
    /**
     * When Guice injects an object, this callback will be notified
     *
     * @param type object type being injected
     * @param obj  object being injected
     */
    public <T> void objectInjected(TypeLiteral<T> type, T obj);

    /**
     * Notification that an object has been injected and the amount of time it to either 
     * construct or retrieve the object.  This call is in addition to the objectInjected
     * above and is currently only called when constructing via ConcurrentProvider
     * 
     * @param type
     * @param obj
     * @param duration
     * @param units
     */
    public <T> void objectInjected(TypeLiteral<T> type, T obj, long duration, TimeUnit units);
    
    /**
     * Called when an object's lifecycle state changes
     *
     * @param obj      the object
     * @param newState new state
     */
    public void stateChanged(Object obj, LifecycleState newState);
    
    /**
     * Notification that an object is being injected.  This call is only made when injecting
     * using ConcurrentProvider
     * @param type
     */
    public <T> void objectInjecting(TypeLiteral<T> type);
}
