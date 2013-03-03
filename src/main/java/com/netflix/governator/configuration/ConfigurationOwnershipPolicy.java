package com.netflix.governator.configuration;

import java.util.Map;

/**
 * Policy to determine if a configuration key is owned by a ConfigurationProvider
 * 
 * @author elandau
 *
 */
public interface ConfigurationOwnershipPolicy {
    public boolean has(ConfigurationKey key, Map<String, String> variables);
}
