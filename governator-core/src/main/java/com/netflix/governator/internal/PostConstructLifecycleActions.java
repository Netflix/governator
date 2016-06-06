package com.netflix.governator.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.netflix.governator.LifecycleAction;
import com.netflix.governator.LifecycleFeature;
import com.netflix.governator.internal.TypeInspector.TypeVisitor;

/**
 * Special LifecycleFeature to support @PostConstruct annotation processing.
 * Note that this feature is implicit in LifecycleModule and therefore does not
 * need to be added using the LifecycleFeature multibinding.
 * 
 * @author elandau
 */
public final class PostConstructLifecycleActions implements LifecycleFeature {
    private static final Logger LOG = LoggerFactory.getLogger(PostConstructLifecycleActions.class);
    public static PostConstructLifecycleActions INSTANCE = new PostConstructLifecycleActions();

    private PostConstructLifecycleActions() {
    }

    @Override
    public List<LifecycleAction> getActionsForType(final Class<?> type) {
        return TypeInspector.accept(type, new PostConstructVisitor());
    }

    @Override
    public String toString() {
        return "PostConstruct";
    }

    private static class PostConstructVisitor implements TypeVisitor, Supplier<List<LifecycleAction>> {
        private Set<String> visitContext = new HashSet<>();
        private LinkedList<LifecycleAction> typeActions = new LinkedList<>();

        @Override
        public boolean visit(final Class<?> clazz) {
            return !clazz.isInterface();
        }

        @Override
        public boolean visit(final Method method) {
            int modifiers = method.getModifiers();
            if (method.isAnnotationPresent(PostConstruct.class)) {
                String methodName = method.getName();
                if (Modifier.isStatic(modifiers)) {
                    LOG.info("invalid static @PostConstruct method {}.{}()", new Object[] { method.getDeclaringClass().getName(), methodName });
                } else if (method.getParameterCount() > 0) {
                    LOG.info("invalid @PostConstruct method {}.{}() with {} parameters", new Object[] { method.getDeclaringClass().getName(), methodName, method.getParameterCount() });
                } else if (Void.TYPE != method.getReturnType()) {
                    LOG.info("invalid @PostConstruct method {}.{}() with return type {}", new Object[] { method.getDeclaringClass().getName(), methodName, method.getReturnType().getName() });
                } else {
                    boolean hasCheckedException=false;
                    if (method.getExceptionTypes().length > 0) {
                        for (Class<?> e : method.getExceptionTypes()) {
                            if (!RuntimeException.class.isAssignableFrom(e)) {
                                LOG.info("invalid @PostConstruct method {}.{}() with checked exception type {}", new Object[] { method.getDeclaringClass().getName(), methodName, e.getName() });
                                hasCheckedException = true;
                            }
                        }
                    }
                    if (!hasCheckedException && !visitContext.contains(methodName)) {
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        // order the members in the list, so superclass
                        // @PostContruct actions are first
                        PostConstructAction postConstructAction = new PostConstructAction(method);
                        LOG.debug("adding action {}", postConstructAction.description);
                        this.typeActions.addFirst(postConstructAction);
                        visitContext.add(methodName);
                    }
                }
            }
            return true;
        }

        @Override
        public boolean visit(Field field) {
            return true;
        }

        @Override
        public List<LifecycleAction> get() {
            return Collections.unmodifiableList(typeActions);
        }

    }

    private static final class PostConstructAction implements LifecycleAction {
        private final Method method;
        private final String description;

        private PostConstructAction(Method method) {
            this.method = method;
            this.description = new StringBuilder().append("PostConstruct@").append(System.identityHashCode(this)).append("[").append(method.getDeclaringClass().getName())
                    .append(".").append(method.getName()).append("()]").toString();
        }

        @Override
        public void call(Object obj)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            LOG.debug("calling action {}", description);
            TypeInspector.invoke(method, obj);
        }

        @Override
        public String toString() {
            return description;
        }
    }

}
