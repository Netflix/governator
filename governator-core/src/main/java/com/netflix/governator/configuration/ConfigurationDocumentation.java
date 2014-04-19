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

import com.google.common.collect.Maps;
import com.google.inject.Singleton;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Used internally to display configuration documentation
 */
@Singleton
public class ConfigurationDocumentation
{
    private final Map<String, Entry> entries = Maps.newConcurrentMap();

    public static class Entry
    {
        public final Field field;
        public final String configurationName;
        public final boolean has;
        public final String defaultValue;
        public final String value;
        public final String documentation;

        private Entry(Field field, String configurationName, boolean has, String defaultValue, String value, String documentation)
        {
            this.field = field;
            this.configurationName = configurationName;
            this.has = has;
            this.defaultValue = defaultValue;
            this.value = value;
            this.documentation = documentation;
        }
    }
    
    public void registerConfiguration(Field field, String configurationName, boolean has, String defaultValue, String value, String documentation)
    {
        entries.put(configurationName, new Entry(field, configurationName, has, defaultValue, value, documentation));
    }

    public Map<String, Entry> getSortedEntries() {
        Map<String, Entry> sortedEntries = Maps.newTreeMap();
        sortedEntries.putAll(entries);
        return sortedEntries;
    }
}
