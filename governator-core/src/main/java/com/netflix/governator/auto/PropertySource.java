package com.netflix.governator.auto;

import com.google.inject.ImplementedBy;

/**
 * Very simple config interface to be used by Conditions to gain access
 * to any type of configuration.  The internal default in AutoModuleBuilder 
 * is to delegate to System properties.
 * 
 * @see PropertiesPropertySource
 * 
 * @author elandau
 * 
 */
@ImplementedBy(DefaultPropertySource.class)
public interface PropertySource {
    /**
     * Get the value of a property or null if not found
     * 
     * @param key
     * @return
     */
    public String get(String key);
    
    /**
     * Get a property value of a specific type
     * 
     * @param key
     * @param type
     * @return
     */
    public <T> T get(String key, Class<T> type);

    /**
     * Get a property value of a specific type while returning a 
     * default value if the property is not set.
     * 
     * @param key
     * @param type
     * @param defaultValue
     * @return
     */
    public <T> T get(String key, Class<T> type, T defaultValue);
}
