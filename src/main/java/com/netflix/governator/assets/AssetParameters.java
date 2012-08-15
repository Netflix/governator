package com.netflix.governator.assets;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import javax.inject.Inject;
import java.util.Map;

/**
 * Holds parameters for an asset
 */
public class AssetParameters implements AssetParametersView
{
    private final Map<Object, Object>  parameters = Maps.newHashMap();

    @Inject
    public AssetParameters()
    {
    }

    /**
     * Set a parameter
     *
     * @param key parameter key
     * @param parameter value
     */
    public<T> void     set(Class<T> key, T parameter)
    {
        internalSet(key, parameter);
    }

    /**
     * Set a parameter using a generic key. E.g.:<br/><br/>
     * <code>
     *     List&lt;String&gt; list = ...;<br/>
     *     parameters.set(new GenericParameterType&lt;List&lt;String&gt;&gt;(){}, list);<br/>
     * </code>
     *
     * @param key parameter key
     * @param parameter value
     */
    public<T> void     set(GenericParameterType<T> key, T parameter)
    {
        internalSet(key.getType(), parameter);
    }

    public void        internalSet(Object key, Object parameter)
    {
        key = Preconditions.checkNotNull(key, "key cannot be null");
        parameter = Preconditions.checkNotNull(parameter, "parameter cannot be null");

        Preconditions.checkState(!parameters.containsKey(key), String.format("A parameter has already been set for that key (%s)", key.getClass().getName()));
        parameters.put(key, parameter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public<T> T        get(Class<T> key)
    {
        Object      value = internalGet(key);
        return key.cast(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public<T> T        get(GenericParameterType<T> key)
    {
        Object      value = internalGet(key.getType());
        return key.cast(value);
    }

    private Object        internalGet(Object key)
    {
        key = Preconditions.checkNotNull(key, "key cannot be null");
        return parameters.get(key);
    }
}
