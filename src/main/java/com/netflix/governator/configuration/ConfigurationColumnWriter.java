package com.netflix.governator.configuration;

import java.io.PrintWriter;
import java.util.Map;

import org.slf4j.Logger;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.netflix.governator.configuration.ConfigurationDocumentation.Entry;

/**
 * Simple implementation of a configuration writer that outputs in column format
 * 
 * @author elandau
 *
 */
public class ConfigurationColumnWriter {
    private final ConfigurationDocumentation config;
    
    @Inject
    public ConfigurationColumnWriter(ConfigurationDocumentation config) {
        this.config = config;
    }
    
    /**
     * Write the documentation table to a logger
     * @param log
     */
    public void output(Logger log)
    {
        Map<String, Entry> entries = config.getSortedEntries();
        
        if ( entries.isEmpty() )
        {
            return;
        }

        ColumnPrinter printer = build(entries);

        log.debug("Configuration Details");
        for ( String line : printer.generate() )
        {
            log.debug(line);
        }
    }

    /**
     * Write the documentation table to System.out
     */
    public void output()
    {
        output(new PrintWriter(System.out));
    }

    /**
     * Output documentation table to a PrintWriter
     * 
     * @param out
     */
    public void output(PrintWriter out)
    {
        Map<String, Entry> entries = config.getSortedEntries();
        
        if ( entries.isEmpty() )
        {
            return;
        }

        ColumnPrinter printer = build(entries);

        out.println("Configuration Details");
        printer.print(out);
    }

    /**
     * Construct a ColumnPrinter using the entries 
     * 
     * @param entries
     * @return
     */
    private ColumnPrinter build(Map<String, Entry> entries)
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

}
