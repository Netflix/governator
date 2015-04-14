package com.netflix.governator.lifecycle.processors;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.ConfigurationVariable;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationMapper;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.guice.LifecycleAnnotationProcessor;
import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.LifecycleState;

@Singleton
public class ConfigurationProviderLifecycleAnnotationProcessor implements LifecycleAnnotationProcessor {

    private final ConfigurationProvider provider;
    private final ConfigurationDocumentation documentation;
    private final ConfigurationMapper mapper;

    @Inject
    public ConfigurationProviderLifecycleAnnotationProcessor(
            ConfigurationProvider provider, 
            ConfigurationDocumentation documentation,
            ConfigurationMapper mapper) {
        this.provider = provider;
        this.documentation = documentation;
        this.mapper = mapper;
    }

    @Override
    public void process(Object obj, LifecycleMethods methods) throws Exception {
        mapper.mapConfiguration(provider, documentation, obj, methods);
    }

    @Override
    public Collection<Class<? extends Annotation>> getFieldAnnotations() {
        return Collections.unmodifiableList(Arrays.<Class<? extends Annotation>>asList(Configuration.class, ConfigurationVariable.class));
    }

    @Override
    public Collection<Class<? extends Annotation>> getMethodAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<? extends Annotation>> getClassAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public LifecycleState getState() {
        return LifecycleState.SETTING_CONFIGURATION;
    }
}
