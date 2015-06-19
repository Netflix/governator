package com.netflix.governator.auto;

/**
 * Very simple config interface to be used by Conditions to gain access
 * to any type of configuration.  The internal default in AutoModuleBuilder 
 * is to delegate to System properties.
 * 
 * @see PropertiesConfig
 * 
 * @author elandau
 *
 */
public interface Config {
    /**
     * Get the value of a property or null if not found
     * 
     * @param key
     * @return
     */
    public String get(String key);
}
