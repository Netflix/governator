/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.lifecycle;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationMapper;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.warmup.WarmUpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LifecycleManagerArguments
{
    private static final Logger             log = LoggerFactory.getLogger(LifecycleManagerArguments.class);

    @VisibleForTesting
    public static final long                DEFAULT_WARM_UP_PADDING_MS = TimeUnit.SECONDS.toMillis(3);

    @Inject
    private ConfigurationProvider           configurationProvider;
    
    @Inject
    private ConfigurationMapper             configurationMapper;

    @Inject
    private ConfigurationDocumentation      configurationDocumentation;
    
    @Inject(optional = true)
    private Set<LifecycleListener>          lifecycleListeners = ImmutableSet.of();

    @Inject(optional = true)
    private Set<ResourceLocator>            resourceLocators = ImmutableSet.of();

    @Inject(optional = true)
    private PostStartArguments              postStartArguments = new PostStartArguments()
    {
        @Override
        public WarmUpErrorHandler getWarmUpErrorHandler()
        {
            return new WarmUpErrorHandler()
            {
                @Override
                public void warmUpError(WarmUpException exception)
                {
                    log.error("warm-up error after LifecycleManager.start() has been called.", exception);
                }
            };
        }

        @Override
        public long getWarmUpPaddingMs()
        {
            return DEFAULT_WARM_UP_PADDING_MS;
        }
    };

    @Inject
    public LifecycleManagerArguments(
            ConfigurationDocumentation configurationDocumentation,
            ConfigurationMapper configurationMapper,
            ConfigurationProvider configurationProvider) 
    {
        this.configurationDocumentation = configurationDocumentation;
        this.configurationMapper = configurationMapper;
        this.configurationProvider = configurationProvider;
    }
    
    public LifecycleManagerArguments() {
        this.configurationDocumentation = new ConfigurationDocumentation();
        this.configurationProvider = new LifecycleConfigurationProviders();
        this.configurationMapper = new DefaultConfigurationMapper();
    }

    public ConfigurationMapper getConfigurationMapper() {
        return configurationMapper;
    }
    
    public void setConfigurationMapper(ConfigurationMapper configurationMapper) {
        this.configurationMapper = configurationMapper;
    }
    
    public ConfigurationProvider getConfigurationProvider()
    {
        return configurationProvider;
    }

    public Collection<LifecycleListener> getLifecycleListeners()
    {
        return lifecycleListeners;
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    public void setLifecycleListeners(Collection<LifecycleListener> lifecycleListeners)
    {
        this.lifecycleListeners = ImmutableSet.copyOf(lifecycleListeners);
    }

    public PostStartArguments getPostStartArguments()
    {
        return postStartArguments;
    }

    public void setPostStartArguments(PostStartArguments postStartArguments)
    {
        this.postStartArguments = postStartArguments;
    }

    public Set<ResourceLocator> getResourceLocators()
    {
        return resourceLocators;
    }

    public void setResourceLocators(Set<ResourceLocator> resourceLocators)
    {
        this.resourceLocators = ImmutableSet.copyOf(resourceLocators);
    }

    public void setConfigurationDocumentation(ConfigurationDocumentation configurationDocumentation) {
        this.configurationDocumentation = configurationDocumentation;
    }
    
    public ConfigurationDocumentation getConfigurationDocumentation() {
        return configurationDocumentation;
    }
}
