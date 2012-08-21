package com.netflix.governator.assets;

import com.google.inject.TypeLiteral;

/**
 * Read only view of asset parameters
 */
public interface AssetParametersView
{
    /**
     * Return the value for the given key or null.
     *
     * @param key key
     * @return value or null
     */
    public<T> T        get(Class<T> key);

    /**
     * Return the value for the given generic key or null. e.g.<br/><br/>
     * <code>
     *     List&lt;String&gt; list = parameters.get(new TypeLiteral&lt;List&lt;String&gt;&gt;(){});
     * </code>
     *
     *
     * @param key key
     * @return value or null
     */
    public<T> T        get(TypeLiteral<? extends T> key);
}
