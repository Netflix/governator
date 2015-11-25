package com.netflix.governator;

import java.lang.reflect.InvocationTargetException;


/**
 * Generic interface for actions to be invoked as part of lifecycle 
 * management.  This includes actions such as PostConstruct, PreDestroy
 * and configuration related mapping.
 * 
 * @see LifecycleFeature
 * 
 * @deprecated Moved to karyon
 */
@Deprecated
public interface LifecycleAction {
    /**
     * Invoke the action on an object.
     * 
     * @param obj
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public void call(Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;
}
