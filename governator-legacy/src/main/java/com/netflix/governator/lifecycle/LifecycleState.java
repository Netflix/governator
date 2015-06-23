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


/**
 * Possible states for a managed object
 */
public enum LifecycleState
{
    /**
     * Not managed, unknown, etc.
     */
    LATENT,

    /**
     * Loading/assigning Resources
     */
    SETTING_RESOURCES,

    /**
     * Calling PreConfiguration methods
     */
    PRE_CONFIGURATION,

    /**
     * Assigning configuration values
     */
    SETTING_CONFIGURATION,

    /**
     * Calling PostConstruct methods
     */
    POST_CONSTRUCTING,

    /**
     * Preparing to call warm-up methods
     */
    PRE_WARMING_UP,

    /**
     * Calling warm-up methods
     */
    WARMING_UP,

    /**
     * Completely ready for use
     */
    ACTIVE,

    /**
     * Calling PreDestroy methods (state will change to LATENT after this)
     */
    PRE_DESTROYING,

    /**
     * There was an exception during warm-up/cool-down for this object
     */
    ERROR
}
