package com.netflix.governator.spi;

import com.google.inject.ImplementedBy;
import com.netflix.governator.PropertiesPropertySource;
import com.netflix.governator.internal.DefaultPropertySource;

/**
 * Very simple config interface to be used by Conditional to gain access
 * to any type of configuration.
 * 
 * @see PropertiesPropertySource
 */
@ImplementedBy(DefaultPropertySource.class)
public interface PropertySource {
    /**
     * Get the value of a property or null if not found
     * 
     * @param key Name of property to fetch 
     * @return Value or null if not found
     */
    public String get(String key);
    
    /**
     * Get the value of a property or default if not found
     * 
     * @param key Name of property to fetch 
     * @param defaultValue
     * @return Value or defaultValue if not found
     */
    public String get(String key, String defaultValue);
    
    /**
     * Get a property value of a specific type
     * 
     * @param key Name of property to fetch 
     * @param type Type of value requested
     * @return Value of the request type or null if not found
     */
    public <T> T get(String key, Class<T> type);

    /**
     * Get a property value of a specific type while returning a 
     * default value if the property is not set.
     * 
     * @param key Name of property to fetch 
     * @param type Type of value requested
     * @param defaultValue Default value to return if key not found
     * @return Value or defaultValue if not found
     */
    public <T> T get(String key, Class<T> type, T defaultValue);
    
    /**
     * Determine if the PropertySource contains the specified property key
     */
    boolean hasProperty(String key);
}
