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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.KeyParser;
import com.netflix.governator.configuration.Property;

class ConfigurationProcessor
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConfigurationProvider configurationProvider;
    private final ConfigurationDocumentation configurationDocumentation;

    ConfigurationProcessor(ConfigurationProvider configurationProvider, ConfigurationDocumentation configurationDocumentation)
    {
        this.configurationProvider = configurationProvider;
        this.configurationDocumentation = configurationDocumentation;
    }

    void assignConfiguration(Object obj, Field field, Map<String, String> contextOverrides) throws Exception
    {
        Configuration configuration = field.getAnnotation(Configuration.class);
        String configurationName = configuration.value();
        ConfigurationKey key = new ConfigurationKey(configurationName, KeyParser.parse(configurationName, contextOverrides));

        Object value = null;

        boolean has = configurationProvider.has(key);
        if ( has )
        {
            try
            {
                if ( Supplier.class.isAssignableFrom(field.getType()) )
                {
                    ParameterizedType type = (ParameterizedType)field.getGenericType();
                    Type actualType = type.getActualTypeArguments()[0];
                    Class<?> actualClass;
                    if (actualType instanceof Class) {
                        actualClass = (Class<?>) actualType;
                    } else if (actualType instanceof ParameterizedType) {
                        actualClass = (Class<?>) ((ParameterizedType) actualType).getRawType();
                    } else {
                        throw new UnsupportedOperationException("Supplier parameter type " + actualType
                                + " not supported (" + field.getName() + ")");
                    }
                    Supplier<?> current = (Supplier<?>)field.get(obj);
                    value = getConfigurationSupplier(field, key, actualClass, current);
                    if ( value == null )
                    {
                        log.error("Field type not supported: " + actualClass + " (" + field.getName() + ")");
                        field = null;
                    }
                }
                else if ( Property.class.isAssignableFrom(field.getType()) )
                {
                    ParameterizedType type = (ParameterizedType)field.getGenericType();
                    Type actualType = type.getActualTypeArguments()[0];
                    Class<?> actualClass;
                    if (actualType instanceof Class) {
                        actualClass = (Class<?>) actualType;
                    } else if (actualType instanceof ParameterizedType) {
                        actualClass = (Class<?>) ((ParameterizedType) actualType).getRawType();
                    } else {
                        throw new UnsupportedOperationException("Supplier parameter type " + actualType
                                + " not supported (" + field.getName() + ")");
                    }
                    Property<?> current = (Property<?>)field.get(obj);
                    value = getConfigurationProperty(field, key, actualClass, current);
                    if ( value == null )
                    {
                        log.error("Field type not supported: " + actualClass + " (" + field.getName() + ")");
                        field = null;
                    }
                }
                else
                {
                    Supplier<?> supplier = getConfigurationSupplier(field, key, field.getType(), Suppliers.ofInstance(field.get(obj)));
                    if ( supplier == null )
                    {
                        log.error("Field type not supported: " + field.getType() + " (" + field.getName() + ")");
                        field = null;
                    }
                    else
                    {
                        value = supplier.get();
                    }
                }
            }
            catch ( IllegalArgumentException e )
            {
                ignoreTypeMismtachIfConfigured(configuration, configurationName, e);
                field = null;
            }
//            catch ( ConversionException e )
//            {
//                ignoreTypeMismtachIfConfigured(configuration, configurationName, e);
//                field = null;
//            }
        }

        if ( field != null )
        {
            String defaultValue;
            if ( Supplier.class.isAssignableFrom(field.getType()) )
            {
                defaultValue = String.valueOf(((Supplier<?>)field.get(obj)).get());
            }
            else
            {
                defaultValue = String.valueOf(field.get(obj));
            }

            String documentationValue;
            if ( has )
            {
                field.set(obj, value);

                documentationValue = String.valueOf(value);
                if ( Supplier.class.isAssignableFrom(field.getType()) )
                {
                    documentationValue = String.valueOf(((Supplier<?>)value).get());
                }
                else
                {
                    documentationValue = String.valueOf(documentationValue);
                }
            }
            else
            {
                documentationValue = "";
            }
            configurationDocumentation.registerConfiguration(field, configurationName, has, defaultValue, documentationValue, configuration.documentation());
        }
    }

    private Property<?> getConfigurationProperty(Field field, ConfigurationKey key, Class<?> type, Property<?> current) {
        return Property.from(getConfigurationSupplier(field, key, type, Property.from(current)));
    }

    @SuppressWarnings("unchecked")
    private Supplier<?> getConfigurationSupplier(final Field field, final ConfigurationKey key, final Class<?> type, Supplier<?> current)
    {
        if ( String.class.isAssignableFrom(type) )
        {
            return configurationProvider.getStringSupplier(key, (String)current.get());
        }
        else if ( Boolean.class.isAssignableFrom(type) || Boolean.TYPE.isAssignableFrom(type) )
        {
            return configurationProvider.getBooleanSupplier(key, (Boolean)current.get());
        }
        else if ( Integer.class.isAssignableFrom(type) || Integer.TYPE.isAssignableFrom(type) )
        {
            return configurationProvider.getIntegerSupplier(key, (Integer)current.get());
        }
        else if ( Long.class.isAssignableFrom(type) || Long.TYPE.isAssignableFrom(type) )
        {
            return configurationProvider.getLongSupplier(key, (Long)current.get());
        }
        else if ( Double.class.isAssignableFrom(type) || Double.TYPE.isAssignableFrom(type) )
        {
            return configurationProvider.getDoubleSupplier(key, (Double)current.get());
        }
        else if ( Date.class.isAssignableFrom(type) )
        {
            return configurationProvider.getDateSupplier(key, (Date)current.get());
        }
        else
        {
            /* Try to deserialize */
            return configurationProvider.getObjectSupplier(key, current.get(), (Class<Object>) type);
        }
    }

    private void ignoreTypeMismtachIfConfigured(Configuration configuration, String configurationName, Exception e)
    {
        if ( configuration.ignoreTypeMismatch() )
        {
            log.info(String.format("Type conversion failed for configuration name %s. This error will be ignored and the field will have the default value if specified. Error: %s", configurationName, e));
        }
        else
        {
            throw Throwables.propagate(e);
        }
    }
}
