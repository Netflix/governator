package com.netflix.governator.configuration;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Implements the deserialization part of {@link ConfigurationProvider} to simplify implementations.
 *
 * Created by jad.naous on 4/2/14.
 */
public abstract class AbstractObjectConfigurationProvider extends DefaultConfigurationProvider {

    private final Logger logger;

    private final ObjectMapper mapper;

    protected AbstractObjectConfigurationProvider() {
        this(null);
    }

    protected AbstractObjectConfigurationProvider(ObjectMapper mapper) {
        if (mapper == null) {
            this.mapper = new ObjectMapper();
        } else {
            this.mapper = mapper;
        }
        this.logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public <T> Property<T> getObjectProperty(final ConfigurationKey key,  final T defaultValue, final Class<T> objectType) {
        return new Property<T>() {
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
