package com.netflix.governator.internal;

import static com.netflix.governator.internal.AbstractLifecycleFeature.TypeVisitor.ElementType.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PreDestroy;

import com.netflix.governator.LifecycleAction;

/**
 * Special AbstractLifecycleFeature to support @PreDestroy annotation processing
 * and java.io.AutoCloseable detection. Note that this feature is implicit in
 * LifecycleModule and therefore does not need to be added using the
 * LifecycleFeature multibinding.
 * 
 * @author elandau
 */
public final class PreDestroyLifecycleActions extends AbstractLifecycleFeature {

    public static PreDestroyLifecycleActions INSTANCE = new PreDestroyLifecycleActions();

    @Override
    public List<LifecycleAction> getActionsForType(final Class<?> type) {
        final List<LifecycleAction> typeActions;
        if (AutoCloseable.class.isAssignableFrom(type)) {
            LifecycleAction closeableAction = new LifecycleAction() {
                @Override
                public void call(Object obj) throws Exception {
                    ((AutoCloseable) obj).close();
                }

                @Override
                public String toString() {
                    return new StringBuilder().append("PreDestroy[").append(type.getName()).append("#").append("close")
                            .append("]").toString();
                }
            };
            typeActions = Collections.singletonList(closeableAction);
        } else {
            typeActions = super.getActionsForType(type);
        }
        return typeActions;
    }

    static class PreDestroyVisitor implements TypeVisitor {
        private Set<String> visitContext = new HashSet<>();

        @Override
        public List<LifecycleAction> getMethodActions(final Class<?> type, final Method method) {
            List<LifecycleAction> typeActions = null;
            int modifiers = method.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isAbstract(modifiers) && method.getParameterCount() == 0
                    && Void.TYPE.equals(method.getReturnType())) {
                if (!visitContext.contains(method.getName())) {
                    if (method.isAnnotationPresent(PreDestroy.class)) {
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        visitContext.add(method.getName());
                        typeActions = Collections.<LifecycleAction> singletonList(new LifecycleAction() {
                            @Override
                            public void call(Object obj)
                                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                                method.invoke(obj);
                            }

                            @Override
                            public String toString() {
                                return new StringBuilder().append("PreDestroy[").append(type.getName()).append("#")
                                        .append(method.getName()).append("]").toString();
                            }
                        });
                    }
                }
            }
            return typeActions != null ? typeActions : Collections.<LifecycleAction> emptyList();

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
    public String toString() {
        return "PreDestroy";
    }

    @Override
    protected TypeVisitor newTypeVisitor() {
        return new PreDestroyVisitor();
    }

}
