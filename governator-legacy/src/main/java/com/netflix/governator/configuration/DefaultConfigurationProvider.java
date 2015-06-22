package com.netflix.governator.configuration;

import java.util.Date;

import com.google.common.base.Supplier;

/**
 * This is a hack in preparation for removing Supplier from the API.  
 * 
 * @author elandau
 *
 */
public abstract class DefaultConfigurationProvider implements ConfigurationProvider {

    @Override
    public boolean has(ConfigurationKey key) {
        return false;
    }

    @Override
    public Supplier<Boolean> getBooleanSupplier(ConfigurationKey key,
            Boolean defaultValue) {
        return Property.from(getBooleanProperty(key, defaultValue));
    }

    @Override
    public Supplier<Integer> getIntegerSupplier(ConfigurationKey key, Integer defaultValue) {
        return Property.from(getIntegerProperty(key, defaultValue));
    }

    @Override
    public Supplier<Long> getLongSupplier(ConfigurationKey key, Long defaultValue) {
        return Property.from(getLongProperty(key, defaultValue));
    }

    @Override
    public Supplier<Double> getDoubleSupplier(ConfigurationKey key, Double defaultValue) {
        return Property.from(getDoubleProperty(key, defaultValue));
    }

    @Override
    public Supplier<String> getStringSupplier(ConfigurationKey key, String defaultValue) {
        return Property.from(getStringProperty(key, defaultValue));
    }

    @Override
    public Supplier<Date> getDateSupplier(ConfigurationKey key, Date defaultValue) {
        return Property.from(getDateProperty(key, defaultValue));
    }

    @Override
    public <T> Supplier<T> getObjectSupplier(ConfigurationKey key, T defaultValue, Class<T> objectType) {
        return Property.from(getObjectProperty(key, defaultValue, objectType));
    }
    
    public abstract Property<Boolean> getBooleanProperty(ConfigurationKey key, Boolean defaultValue);
    public abstract Property<Integer> getIntegerProperty(ConfigurationKey key, Integer defaultValue);
    public abstract Property<Long> getLongProperty(ConfigurationKey key, Long defaultValue);
    public abstract Property<Double> getDoubleProperty(ConfigurationKey key, Double defaultValue);
    public abstract Property<String> getStringProperty(ConfigurationKey key, String defaultValue);
    public abstract Property<Date> getDateProperty(ConfigurationKey key, Date defaultValue);
    public abstract <T> Property<T> getObjectProperty(ConfigurationKey key, T defaultValue, Class<T> objectType);
}
