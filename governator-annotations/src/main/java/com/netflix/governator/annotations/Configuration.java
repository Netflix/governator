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

package com.netflix.governator.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field as a configuration item. Governator will auto-assign the value based
 * on the {@link #value()} of the annotation via the set com.netflix.governator.configurationConfigurationProvider.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Configuration
{
    /**
     * @return name/key of the config to assign
     */
    String      value();

    /**
     * @return user displayable description of this configuration
     */
    String      documentation() default "";

    /**
     * If the value of the property defined for this config is not the same or can not be converted to the
     * required type, setting this to <code>true</code> will ignore such a value. In such a case, the target field
     * will contain the default value if defined.<br/>
     * The error during configuration is deemed as a type mismatch if the exception thrown by the
     * com.netflix.governator.configurationConfigurationProvider is one amongst:
     * <ul>
     * <li>{@link IllegalArgumentException}</li>
     * <li>org.apache.commons.configuration.ConversionException</li>
     </ul>
     *
     * @return <code>true</code> if type mismatch must be ignored. <code>false</code> otherwise. Default value is
     * <code>false</code>
     */
    boolean ignoreTypeMismatch() default false;
}
