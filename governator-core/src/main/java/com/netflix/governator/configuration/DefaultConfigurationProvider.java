package com.netflix.governator.configuration;

import java.util.Date;

import com.google.common.base.Supplier;

/**
 * This is a hack in preparation for removing Supplier from the API.
 * 
 * @author elandau
 *
 */
public class DefaultConfigurationProvider implements ConfigurationProvider {

    @Override
    public boolean has(ConfigurationKey key) {
        return false;
    }

    @Override
    public Supplier<Boolean> getBooleanSupplier(ConfigurationKey key,
            Boolean defaultValue) {
        return null;
    }

    @Override
    public Supplier<Integer> getIntegerSupplier(ConfigurationKey key,
            Integer defaultValue) {
        return null;
    }

    @Override
    public Supplier<Long> getLongSupplier(ConfigurationKey key,
            Long defaultValue) {
        return null;
    }

    @Override
    public Supplier<Double> getDoubleSupplier(ConfigurationKey key,
            Double defaultValue) {
        return null;
    }

    @Override
    public Supplier<String> getStringSupplier(ConfigurationKey key,
            String defaultValue) {
        return null;
    }

    @Override
    public Supplier<Date> getDateSupplier(ConfigurationKey key,
            Date defaultValue) {
        return null;
    }

    @Override
    public <T> Supplier<T> getObjectSupplier(ConfigurationKey key,
            T defaultValue, Class<T> objectType) {
        return null;
    }
}
