package com.netflix.governator.auto;

import java.util.Properties;

import javax.inject.Singleton;

import com.google.inject.Module;
import com.google.inject.Provides;
import com.netflix.governator.DefaultModule;

public class PropertiesConfig extends AbstractPropertySource {
    private Properties props;

    public PropertiesConfig(Properties props) {
        this.props = props;
    }

    public static PropertiesConfig from(Properties props) {
        return new PropertiesConfig(props);
    }
    
    public static Module toModule(final Properties props) {
        return new DefaultModule() {
            @Provides
            @Singleton
            public PropertySource getConfig() {
                return new PropertiesConfig(props);
            }
        };
    }
    
    @Override
    public String get(String key) {
        return props.getProperty(key);
    }
}
