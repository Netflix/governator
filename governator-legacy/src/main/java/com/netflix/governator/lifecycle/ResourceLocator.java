package com.netflix.governator.lifecycle;

import com.netflix.governator.guice.BootstrapBinder;
import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.naming.NameNotFoundException;

/**
 * Used to load {@link Resource} and {@link Resources} annotated objects. Bind
 * one or more instances via {@link BootstrapBinder#bindResourceLocator()}.
 */
public interface ResourceLocator
{
    /**
     * Load and return the given resource. If you cannot or do not wish to load
     * it, pass on to the next locator in the chain. NOTE: the default ResourceLocator
     * merely throws {@link NameNotFoundException}.
     *
     * @param resource the resource to load - NOTE: type() and name() will have been adjusted if defaults were used.
     * @param nextInChain the next locator in the chain (never <code>null</code>)
     * @return the loaded object
     * @throws Exception errors
     */
    public Object locate(Resource resource, ResourceLocator nextInChain) throws Exception;
}
