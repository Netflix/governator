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

package com.netflix.governator.configuration;

import java.util.Map;

/**
 * Policy to determine if a configuration key is owned by a ConfigurationProvider
 *
 * @author elandau
 */
public interface ConfigurationOwnershipPolicy
{
    /**
     * Return true if there is a configuration value set for the given key + variables
     *
     * @param key configuration key
     * @param variableValues map of variable names to values
     * @return true/false
     */
    public boolean has(ConfigurationKey key, Map<String, String> variableValues);
}
