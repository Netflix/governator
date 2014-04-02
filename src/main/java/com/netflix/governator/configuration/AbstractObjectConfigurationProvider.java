package com.netflix.governator.configuration;

import com.google.common.base.Supplier;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Implements the deserialization part of {@link ConfigurationProvider} to simplify implementations.
 *
 * Created by jad.naous on 4/2/14.
 */
public abstract class AbstractObjectConfigurationProvider implements ConfigurationProvider {

    private final Logger logger;

    private final ObjectMapper mapper;

    protected AbstractObjectConfigurationProvider(ObjectMapper mapper) {
        this.mapper = mapper;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public <T> Supplier<T> getObjectSupplier(
            final ConfigurationKey key, final T defaultValue, final Class<T> objectType) {
        return new Supplier<T>() {
            @Override
            public T get() {
                String serialized = getStringSupplier(key, null).get();
                if (serialized == null || serialized.length() == 0) {
                    return defaultValue;
                }
                try {
                    return mapper.readValue(serialized, objectType);
                } catch (IOException e) {
                    logger.warn("Could not deserialize configuration with key " + key.getRawKey()
                            + " to type " + objectType, e);
                    return defaultValue;
                }
            }
        };
    }

}
