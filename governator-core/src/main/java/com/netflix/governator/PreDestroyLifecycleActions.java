package com.netflix.governator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.PreDestroy;

/**
 * Special AbstractLifecycleFeature to support @PreDestroy annotation processing.
 * Note that this feature is implicit in LifecycleModule and therefore does not need 
 * to be added using the LifecycleFeature multibinding.
 * 
 * @deprecated Moved to karyon
 */
@Deprecated
final class PreDestroyLifecycleActions extends AbstractLifecycleFeature {

    static PreDestroyLifecycleActions INSTANCE = new PreDestroyLifecycleActions();
    
    @Override
    protected LifecycleAction getMethodAction(final Class<?> type, final Method method) {
        if (null != method.getAnnotation(PreDestroy.class)) {
            method.setAccessible(true);
            return new LifecycleAction() {
                @Override
                public void call(Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                    method.invoke(obj);
                }
                
                @Override
                public String toString() {
                    return new StringBuilder()
                        .append("PreDestroy[")
                        .append(type.getName())
                        .append("#")
                        .append(method.getName())
                        .append("]")
                        .toString();
                }
            };
        }
        return NONE;
    }

    @Override
    public String toString() {
        return "PreDestroy";
    }

}
