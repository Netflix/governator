package com.netflix.governator.assets;

import com.google.common.base.Preconditions;
import com.google.inject.TypeLiteral;

public class AssetParameterKey<T>
{
    private final String            assetName;
    private final TypeLiteral<T>    type;

    public AssetParameterKey(String assetName, TypeLiteral<T> type)
    {
        this.assetName = Preconditions.checkNotNull(assetName, "assetName cannot be null");
        this.type = Preconditions.checkNotNull(type, "type cannot be null");
    }

    public AssetParameterKey(String assetName, Class<T> type)
    {
        this(assetName, TypeLiteral.get(type));
    }

    public String getAssetName()
    {
        return assetName;
    }

    public TypeLiteral<T> getType()
    {
        return type;
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

        AssetParameterKey that = (AssetParameterKey)o;

        if ( !assetName.equals(that.assetName) )
        {
            return false;
        }
        //noinspection RedundantIfStatement
        if ( !type.equals(that.type) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = assetName.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
