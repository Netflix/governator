package com.netflix.governator.assets;

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
     *     List&lt;String&gt; list = parameters.get(new GenericParameterType&lt;List&lt;String&gt;&gt;(){});
     * </code>
     *
     * @param key key
     * @return value or null
     */
    public<T> T        get(GenericParameterType<T> key);
}
