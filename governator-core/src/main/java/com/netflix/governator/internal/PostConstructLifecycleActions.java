package com.netflix.governator.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.netflix.governator.LifecycleAction;

/**
 * Special AbstractLifecycleFeature to support @PostConstruct annotation processing.
 * Note that this feature is implicit in LifecycleModule and therefore does not need 
 * to be added using the LifecycleFeature multibinding.
 * 
 * @author elandau
 */
public final class PostConstructLifecycleActions extends AbstractLifecycleFeature<Set<String>> {

    public static PostConstructLifecycleActions INSTANCE = new PostConstructLifecycleActions();
    
    private PostConstructLifecycleActions() {
        super(IntrospectionFlag.METHOD, IntrospectionFlag.SUPERCLASS);
    }
    
    @Override
    protected List<LifecycleAction> getMethodActions(Set<String> typeContext, final Class<?> type, final Method method) {
    	int modifiers = method.getModifiers();
		if (!Modifier.isStatic(modifiers) && !Modifier.isAbstract(modifiers) && method.getParameterCount() == 0  && Void.TYPE.equals(method.getReturnType())) {
			if (!typeContext.contains(method.getName())) {
		        if (null != method.getAnnotation(PostConstruct.class)) {
			        if (!method.isAccessible()) {
			        	method.setAccessible(true);
			        }
			        typeContext.add(method.getName());	
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
	        }
		}
        return Collections.emptyList();
    }
    
	@Override
	protected Set<String> newTypeContext() {
		return new HashSet<String>();
	}
	
    @Override
	public List<LifecycleAction> getActionsForType(final Class<?> type) {
    	List<LifecycleAction> typeActions = super.getActionsForType(type);
    	Collections.reverse(typeActions); // apply actions in reverse order; super->child
    	return typeActions;
    }
    
    @Override
    public String toString() {
        return "PostConstruct";
    }

}
