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
 * @deprecated 2016-05-19 This class is being deprecated in favor of using {@literal @}PostConstruct
 * or running initialization code in the constructor.  While {@literal @}WarmUp did promise to help 
 * with parallel initialization of singletons it resulted in excessive complexity and invalidated DI
 * expectations that a class is fully initialized by the time it is injected.  WarmUp methods are 
 * now treated exactly like PostConstruct and are therefore guaranteed to have been executed by the
 * time an object is injected.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Deprecated
public @interface WarmUp
{
}
