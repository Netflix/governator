package com.netflix.governator.configuration;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuration property ownership policy that checks a property against a 
 * regex to determine if a ConfigurationProvider owns the property.  Use this 
 * for dynamic configuration to give ownership in a situations where the 
 * configuration key may not exist in the provider at startup
 * @author elandau
 *
 */
public class RegexConfigurationOwnershipPolicy implements ConfigurationOwnershipPolicy {
    private Pattern pattern;
    
    public RegexConfigurationOwnershipPolicy(String regex) {
        pattern = Pattern.compile(regex);
    }
    
    @Override
    public boolean has(ConfigurationKey key, Map<String, String> variables) {
        return pattern.matcher(key.getKey(variables)).matches();
    }
}
