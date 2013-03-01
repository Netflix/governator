package com.netflix.governator.configuration;

import java.util.Map;

/**
 * Configuration policy to use when a ConfigurationProvider owns all 
 * configuration properties.  Use this for dynamic configuration to give 
 * ownership in a situations where the configuration key may not exist in the 
 * provider at startup
 * 
 * @author elandau
 */
public class OwnsAllConfigurationOwnershipPolicy implements ConfigurationOwnershipPolicy {
    public static OwnsAllConfigurationOwnershipPolicy instance = new OwnsAllConfigurationOwnershipPolicy();
    public static OwnsAllConfigurationOwnershipPolicy getInstance() {
        return instance;
    }
    
    @Override
    public boolean has(ConfigurationKey key, Map<String, String> variables) {
        return true;
    }
}
