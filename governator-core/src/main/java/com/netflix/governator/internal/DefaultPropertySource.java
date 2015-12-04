package com.netflix.governator.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.governator.AbstractPropertySource;

/**
 * PropertySource based on system and environment properties with 
 * system properties having precedence. 
 * 
 * @author elandau
 *
 */
@Singleton
public final class DefaultPropertySource extends AbstractPropertySource {

    @Inject
    public DefaultPropertySource() {
    }
    
    @Override
    public String get(String key) {
        return get(key, (String)null);
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
            if (value == null)
                return defaultValue;
        }
        return value;
    }
}
