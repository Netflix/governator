package com.netflix.governator;

import java.util.Properties;

import javax.inject.Singleton;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.netflix.governator.spi.PropertySource;

public final class PropertiesPropertySource extends AbstractPropertySource implements Module {
    private Properties props;

    public PropertiesPropertySource(Properties props) {
        this.props = props;
    }

    public PropertiesPropertySource() {
        this(new Properties());
    }

    public static PropertiesPropertySource from(Properties props) {
        return new PropertiesPropertySource(props);
    }
    
    public PropertiesPropertySource setProperty(String key, String value) {
        props.setProperty(key, value);
        return this;
    }
    
    public boolean hasProperty(String key, String value) {
        return props.containsKey(key);
    }
    
    public static Module toModule(final Properties props) {
        return new SingletonModule() {
            @Provides
            @Singleton
            public PropertySource getConfig() {
                return new PropertiesPropertySource(props);
            }
        };
    }
    
    @Override
    public String get(String key) {
        return props.getProperty(key);
    }

    @Override
    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(PropertySource.class).toInstance(this);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        throw new RuntimeException("Only one PropertiesModule may be installed");
    }


}
