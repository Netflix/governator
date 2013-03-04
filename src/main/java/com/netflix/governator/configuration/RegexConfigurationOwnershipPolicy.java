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
import java.util.regex.Pattern;

/**
 * Configuration property ownership policy that checks a property against a
 * regex to determine if a ConfigurationProvider owns the property.  Use this
 * for dynamic configuration to give ownership in a situations where the
 * configuration key may not exist in the provider at startup
 *
 * @author elandau
 */
public class RegexConfigurationOwnershipPolicy implements ConfigurationOwnershipPolicy
{
    private Pattern pattern;

    public RegexConfigurationOwnershipPolicy(String regex)
    {
        pattern = Pattern.compile(regex);
    }

    @Override
    public boolean has(ConfigurationKey key, Map<String, String> variables)
    {
        return pattern.matcher(key.getKey(variables)).matches();
    }
}
