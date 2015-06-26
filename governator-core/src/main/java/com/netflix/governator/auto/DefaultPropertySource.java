package com.netflix.governator.auto;

import javax.inject.Singleton;

@Singleton
public class DefaultPropertySource extends AbstractPropertySource {

    @Override
    public String get(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }
}
