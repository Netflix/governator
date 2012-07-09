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

package com.netflix.governator.inject.guice;

import java.io.Serializable;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class ClassHolderImp implements ClassHolder, Serializable
{
    private final Class<?>      clazz;

    public ClassHolderImp(Class<?> clazz)
    {
        this.clazz = clazz;
    }

    @Override
    public Class<?> clazz()
    {
        return clazz;
    }

    @Override
    public Class<? extends Annotation> annotationType()
    {
        return ClassHolder.class;
    }

    @Override
    public boolean equals(Object o)
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ClassHolderImp that = (ClassHolderImp)o;

        //noinspection RedundantIfStatement
        if ( !clazz.equals(that.clazz) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return clazz.hashCode();
    }
}
