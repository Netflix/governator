package com.netflix.governator.configuration;

public class ConfigurationOwnershipPolicies {
    public static ConfigurationOwnershipPolicy ownesAll() {
        return OwnsAllConfigurationOwnershipPolicy.getInstance();
    }
    
    public static ConfigurationOwnershipPolicy ownesByRegex(String regex) {
        return new RegexConfigurationOwnershipPolicy(regex);
    }
}
