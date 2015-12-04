package com.netflix.governator.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import com.netflix.governator.LifecycleAction;

/**
 * Special AbstractLifecycleFeature to support @PostConstruct annotation processing.
 * Note that this feature is implicit in LifecycleModule and therefore does not need 
 * to be added using the LifecycleFeature multibinding.
 * 
 * @author elandau
 */
public final class PostConstructLifecycleActions extends AbstractLifecycleFeature {

    public static PostConstructLifecycleActions INSTANCE = new PostConstructLifecycleActions();
    
    private PostConstructLifecycleActions() {
        
    }
    
    @Override
    protected List<LifecycleAction> getMethodActions(final Class<?> type, final Method method) {
        if (null != method.getAnnotation(PostConstruct.class)) {
            method.setAccessible(true);
            return Collections.<LifecycleAction>singletonList(new LifecycleAction() {
                @Override
                public void call(Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                    method.invoke(obj);
                }
                
                @Override
                public String toString() {
                    return new StringBuilder()
                        .append("PostConstruct[")
                        .append(type.getName())
                        .append("#")
                        .append(method.getName())
                        .append("]")
                        .toString();
                }
            });
        }
        return Collections.emptyList();
    }
    
    @Override
    public String toString() {
        return "PostConstruct";
    }

}
