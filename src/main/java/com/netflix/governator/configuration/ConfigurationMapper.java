package com.netflix.governator.configuration;

import com.google.inject.ImplementedBy;
import com.netflix.governator.lifecycle.DefaultConfigurationMapper;
import com.netflix.governator.lifecycle.LifecycleMethods;

/**
 * Interface definition for mapping a configuration on an instance
 * 
 * TODO:  Ideally ConfigurationProvider and ConfigurationDocumentation should 
 *        be specific to the specific configuration mapper implementation
 * 
 * @author elandau
 */
@ImplementedBy(DefaultConfigurationMapper.class)
public interface ConfigurationMapper {
    void mapConfiguration(
            ConfigurationProvider configurationProvider, 
            ConfigurationDocumentation configurationDocumentation, 
            Object obj, 
            LifecycleMethods methods) throws Exception;
}
