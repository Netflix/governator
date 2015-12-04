package com.netflix.governator;

public class GovernatorFeature<T> {
    private final String key;
    private final T defaultValue;
    
    public static <T> GovernatorFeature<T> create(String key, T defaultValue) {
        return new GovernatorFeature<T>(key, defaultValue);
    }
    
    public GovernatorFeature(String key, T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
    
    public String getKey() {
        return key;
    }
    
    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        return (Class<T>) defaultValue.getClass();
    }
    
    public T getDefaultValue() {
        return defaultValue;
    }
}
