package com.netflix.governator.lifecycle;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;

/**
 * Wrapper listener that forwards to the provided listener only when the obj is in one of the
 * specified base packages.
 */
public class FilteredLifecycleListener implements LifecycleListener
{
    private final ImmutableSet<String> packages;
    private final LifecycleListener listener;

    /**
     * @param listener actual listener
     * @param basePackages set of base packages
     */
    public FilteredLifecycleListener(LifecycleListener listener, String... basePackages)
    {
        this(listener, Sets.newHashSet(Arrays.asList(basePackages)));
    }

    /**
     * @param listener actual listener
     * @param basePackages set of base packages
     */
    public FilteredLifecycleListener(LifecycleListener listener, Collection<String> basePackages)
    {
        this.listener = listener;
        packages = ImmutableSet.copyOf(basePackages);
    }

    @Override
    public void objectInjected(Object obj)
    {
        if ( isInPackages(obj) )
        {
            listener.objectInjected(obj);
        }
    }

    @Override
    public void stateChanged(Object obj, LifecycleState newState)
    {
        if ( isInPackages(obj) )
        {
            listener.stateChanged(obj, newState);
        }
    }

    private boolean isInPackages(Object obj)
    {
        if ( obj != null )
        {
            for ( String p : packages )
            {
                if ( obj.getClass().getPackage().getName().startsWith(p) )
                {
                    return true;
                }
            }
        }
        return false;
    }
}
