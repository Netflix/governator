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
import org.slf4j.Logger;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Used internally to display configuration documentation
 */
public class ConfigurationDocumentation
{
    private final Map<String, Entry> entries = Maps.newConcurrentMap();

    private static class Entry
    {
        private final Field field;
        private final String configurationName;
        private final boolean has;
        private final String defaultValue;
        private final String value;
        private final String documentation;

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

    public void output(Logger log)
    {
        if ( entries.size() == 0 )
        {
            return;
        }

        ColumnPrinter printer = build();

        log.debug("Configuration Details");
        for ( String line : printer.generate() )
        {
            log.debug(line);
        }
    }

    public void output()
    {
        output(new PrintWriter(System.out));
    }

    public void output(PrintWriter out)
    {
        if ( entries.size() == 0 )
        {
            return;
        }

        ColumnPrinter printer = build();

        out.println("Configuration Details");
        printer.print(out);
    }

    private ColumnPrinter build()
    {
        ColumnPrinter printer = new ColumnPrinter();

        printer.addColumn("PROPERTY");
        printer.addColumn("FIELD");
        printer.addColumn("DEFAULT");
        printer.addColumn("VALUE");
        printer.addColumn("DESCRIPTION");

        Map<String, Entry> sortedEntries = Maps.newTreeMap();
        sortedEntries.putAll(entries);

        for ( Entry entry : sortedEntries.values() )
        {
            printer.addValue(0, entry.configurationName);
            printer.addValue(1, entry.field.getDeclaringClass().getName() + "#" + entry.field.getName());
            printer.addValue(2, entry.defaultValue);
            printer.addValue(3, entry.has ? entry.value : "");
            printer.addValue(4, entry.documentation);
        }
        return printer;
    }

    public void clear()
    {
        entries.clear();
    }
}
