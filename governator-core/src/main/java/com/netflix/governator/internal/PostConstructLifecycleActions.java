package com.netflix.governator.internal;

import static com.netflix.governator.internal.AbstractLifecycleFeature.TypeVisitor.ElementType.METHOD;
import static com.netflix.governator.internal.AbstractLifecycleFeature.TypeVisitor.ElementType.SUPERCLASS;

import java.lang.reflect.Field;
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
 * Special AbstractLifecycleFeature to support @PostConstruct annotation
 * processing. Note that this feature is implicit in LifecycleModule and
 * therefore does not need to be added using the LifecycleFeature multibinding.
 * 
 * @author elandau
 */
public final class PostConstructLifecycleActions extends AbstractLifecycleFeature {

    public static PostConstructLifecycleActions INSTANCE = new PostConstructLifecycleActions();

    static class PostConstructVisitor implements TypeVisitor {
        private Set<String> visitContext = new HashSet<>();

        @Override
        public List<LifecycleAction> getMethodActions(final Class<?> type, final Method method) {
            int modifiers = method.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isAbstract(modifiers) && method.getParameterCount() == 0
                    && Void.TYPE.equals(method.getReturnType())) {
                if (!visitContext.contains(method.getName())) {
                    if (null != method.getAnnotation(PostConstruct.class)) {
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        visitContext.add(method.getName());
                        return Collections.<LifecycleAction> singletonList(new LifecycleAction() {
                            @Override
                            public void call(Object obj)
                                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                                method.invoke(obj);
                            }

                            @Override
                            public String toString() {
                                return new StringBuilder().append("PostConstruct[").append(type.getName()).append("#")
                                        .append(method.getName()).append("]").toString();
                            }
                        });
                    }
                }
            }
            return Collections.emptyList();
        }

        @Override
        public List<LifecycleAction> getFieldActions(Class<?> type, Field field) {
            return Collections.emptyList();
        }

        @Override
        public boolean accept(ElementType elementType) {
            return elementType == METHOD || elementType == SUPERCLASS;
        }

    }

    @Override
    protected TypeVisitor newTypeVisitor() {
        return new PostConstructVisitor();
    }

    @Override
    public List<LifecycleAction> getActionsForType(final Class<?> type) {
        List<LifecycleAction> typeActions = super.getActionsForType(type);
        Collections.reverse(typeActions); // apply actions in reverse order;
                                          // super->child
        return typeActions;
    }

    @Override
    public String toString() {
        return "PostConstruct";
    }

}
