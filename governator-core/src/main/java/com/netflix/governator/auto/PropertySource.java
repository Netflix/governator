package com.netflix.governator.auto;

import com.google.inject.ImplementedBy;

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
@ImplementedBy(DefaultPropertySource.class)
public interface PropertySource {
    /**
     * Get the value of a property or null if not found
     * 
     * @param key
     * @return
     */
    public String get(String key);
    
    public <T> T get(String key, Class<T> type);
    
    public <T> T get(String key, Class<T> type, T defaultValue);
}
