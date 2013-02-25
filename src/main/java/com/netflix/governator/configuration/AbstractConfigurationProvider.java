package com.netflix.governator.configuration;

import com.google.common.base.Supplier;

public abstract class AbstractConfigurationProvider implements ConfigurationProvider{
    
    @Override
    public Supplier<Boolean> getBooleanSupplier(final ConfigurationKey key, final Boolean defaultValue) {
        return new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                Boolean value = getBoolean(key);
                if (value == null)
                    return defaultValue;
                return value;
            }
        };
    }

    @Override
    public Supplier<Integer> getIntegerSupplier(final ConfigurationKey key, final Integer defaultValue) {
        return new Supplier<Integer>() {
            @Override
            public Integer get() {
                Integer value = getInteger(key);
                if (value == null)
                    return defaultValue;
                return value;
            }
        };
    }

    @Override
    public Supplier<Long> getLongSupplier(final ConfigurationKey key, final Long defaultValue) {
        return new Supplier<Long>() {
            @Override
            public Long get() {
                Long value = getLong(key);
                if (value == null)
                    return defaultValue;
                return value;
            }
        };
    }

    @Override
    public Supplier<Double> getDoubleSupplier(final ConfigurationKey key, final Double defaultValue) {
        return new Supplier<Double>() {
            @Override
            public Double get() {
                Double value = getDouble(key);
                if (value == null)
                    return defaultValue;
                return value;
            }
        };
    }

    @Override
    public Supplier<String> getStringSupplier(final ConfigurationKey key, final String defaultValue) {
        return new Supplier<String>() {
            @Override
            public String get() {
                String value = getString(key);
                if (value == null)
                    return defaultValue;
                return value;
            }
        };
    }
}
