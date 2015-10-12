/*
 * Copyright 2012 Netflix, Inc.
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
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a class as a singleton. Governator will auto-bind it as an eager singleton
 * @deprecated  2015-10-10 AutoBindSingleton is deprecated in favor of bindings in a Guice Module.
 *              See https://github.com/Netflix/governator/wiki/Auto-Binding
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
@Deprecated
public @interface AutoBindSingleton
{
    /**
     * By default, AutoBindSingleton binds to the class that has the annotation. However,
     * you can set the value to any base class/interface that you want to bind to. You can
     * bind to generic base classes/interfaces by specifying the raw type (i.e. <code>@AutoBindSingleton(List.class)</code>
     * for <code>List&lt;String&gt;</code>)
     *
     * @return base class/interface to bind to
     */
    Class<?>  value() default AutoBindSingleton.class;

    /**
     * This is a synonym for {@link #value()}. It exists to make the annotation
     * more legible if other annotation values are used. NOTE: it is an error to
     * specify both {@link #value()} and {@link #baseClass()}
     *
     * @return base class/interface to bind to
     */
    Class<?>  baseClass() default AutoBindSingleton.class;

    /**
     * If true, instances are gathered into a Set using Guice's Multibinder. You must
     * also set a base class using either {@link #value()} or {@link #baseClass()}
     *
     * @return true/false
     */
    boolean multiple() default false;
    
    /**
     * If true, instances will be created eagerly.
     * @return
     */
    boolean eager() default true;
}
