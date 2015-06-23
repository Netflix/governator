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
 * Convenience factory for getting standard ownership policies
 */
public class ConfigurationOwnershipPolicies
{
    /**
     * Return an ownership policy that returns true for {@link ConfigurationOwnershipPolicy#has(ConfigurationKey, Map)}
     *
     * @return policy
     */
    public static ConfigurationOwnershipPolicy ownsAll()
    {
        return new ConfigurationOwnershipPolicy()
        {
            @Override
            public boolean has(ConfigurationKey key, Map<String, String> variables)
            {
                return true;
            }
        };
    }

    /**
     * Return an ownership policy that returns true for {@link ConfigurationOwnershipPolicy#has(ConfigurationKey, Map)}
     * when the given regular expression matches
     *
     * @return regex policy
     */
    public static ConfigurationOwnershipPolicy ownsByRegex(String regex)
    {
        return new RegexConfigurationOwnershipPolicy(regex);
    }

    private ConfigurationOwnershipPolicies()
    {
    }
}
