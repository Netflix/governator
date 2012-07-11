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

package com.netflix.governator.assets;

import com.google.common.base.Preconditions;

public class AssetLoaderBinding
{
    private final Class<? extends AssetLoader>  loaderClass;
    private final String                        name;

    public static final String  DEFAULT_BINDING_NAME = "";

    public AssetLoaderBinding(Class<? extends AssetLoader> loaderClass)
    {
        this(loaderClass, DEFAULT_BINDING_NAME);
    }

    public AssetLoaderBinding(Class<? extends AssetLoader> loaderClass, String name)
    {
        this.loaderClass = Preconditions.checkNotNull(loaderClass, "loaderClass cannot be null");
        this.name = Preconditions.checkNotNull(name, "name cannot be null");
    }

    public Class<? extends AssetLoader> getLoaderClass()
    {
        return loaderClass;
    }

    public String getName()
    {
        return name;
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

        AssetLoaderBinding that = (AssetLoaderBinding)o;

        if ( !loaderClass.equals(that.loaderClass) )
        {
            return false;
        }
        //noinspection RedundantIfStatement
        if ( !name.equals(that.name) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = loaderClass.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "AssetLoaderBinding{" +
            "loaderClass=" + loaderClass +
            ", name='" + name + '\'' +
            '}';
    }
}
