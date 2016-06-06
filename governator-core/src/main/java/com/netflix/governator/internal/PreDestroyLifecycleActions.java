package com.netflix.governator.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.netflix.governator.LifecycleAction;
import com.netflix.governator.LifecycleFeature;
import com.netflix.governator.internal.TypeInspector.TypeVisitor;

/**
 * Special LifecycleFeature to support @PreDestroy annotation processing and
 * java.lang.AutoCloseable detection. Note that this feature is implicit in
 * LifecycleModule and therefore does not need to be added using the
 * LifecycleFeature multibinding.
 * 
 * @author elandau
 */
public final class PreDestroyLifecycleActions implements LifecycleFeature {
    private static final Logger LOG = LoggerFactory.getLogger(PreDestroyLifecycleActions.class);
    public static PreDestroyLifecycleActions INSTANCE = new PreDestroyLifecycleActions();

    private PreDestroyLifecycleActions() {
    }

    @Override
    public List<LifecycleAction> getActionsForType(final Class<?> type) {
        return TypeInspector.accept(type, new PreDestroyVisitor());
    }

    @Override
    public String toString() {
        return "PreDestroy";
    }

    private static class PreDestroyVisitor implements TypeVisitor, Supplier<List<LifecycleAction>> {
        private Set<String> visitContext = new HashSet<>();
        private List<LifecycleAction> typeActions = new ArrayList<>();

        @Override
        public boolean visit(final Class<?> clazz) {
            boolean continueVisit = !clazz.isInterface();
            if (continueVisit && AutoCloseable.class.isAssignableFrom(clazz)) {
                AutoCloseableLifecycleAction closeableAction = new AutoCloseableLifecycleAction(
                        clazz.asSubclass(AutoCloseable.class));
                LOG.debug("adding action {}", closeableAction.description);
                typeActions.add(closeableAction);
                continueVisit = false;
            }
            return continueVisit;
        }

        @Override
        public boolean visit(final Method method) {

            if (method.isAnnotationPresent(PreDestroy.class)) {
                final int modifiers = method.getModifiers();
                String methodName = method.getName();
                if (Modifier.isStatic(modifiers)) {
                    LOG.info("invalid static @PreDestroy method {}.{}()", new Object[] { method.getDeclaringClass().getName(), methodName });
                } else if (method.getParameterCount() > 0) {
                    LOG.info("invalid @PreDestroy method {}.{}() with {} parameters", new Object[] { method.getDeclaringClass().getName(), methodName, method.getParameterCount() });
                } else if (Void.TYPE != method.getReturnType()) {
                    LOG.info("invalid @PreDestroy method {}.{}() with return type {}", new Object[] { method.getDeclaringClass().getName(), methodName, method.getReturnType().getName() });
                } else {
                    boolean hasCheckedException=false;
                    if (method.getExceptionTypes().length > 0) {
                        for (Class<?> e : method.getExceptionTypes()) {
                            if (!RuntimeException.class.isAssignableFrom(e)) {
                                LOG.info("invalid @PreDestroy method {}.{}() with checked exception type {}", new Object[] { method.getDeclaringClass().getName(), methodName, e.getName() });
                                hasCheckedException = true;
                            }
                        }
                    }
                    if (!hasCheckedException && !visitContext.contains(methodName)) {
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        DestroyLifecycleAction destroyAction = new DestroyLifecycleAction(method);
                        LOG.debug("adding action {}", destroyAction.description);
                        typeActions.add(destroyAction);
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

    private static final class DestroyLifecycleAction implements LifecycleAction {
        private final Method method;
        private final String description;

        private DestroyLifecycleAction(Method method) {
            this.method = method;
            this.description = new StringBuilder().append("PreDestroy@").append(System.identityHashCode(this)).append("[").append(method.getDeclaringClass().getName())
                    .append(".").append(method.getName()).append("()]").toString();
        }

        @Override
        public void call(Object obj)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            LOG.debug("calling action {}", description);
            try {
                method.invoke(obj);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                }
                else if (cause instanceof Error) {
                    throw (Error)cause;
                }
                throw ite;
            }
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private static final class AutoCloseableLifecycleAction implements LifecycleAction {
        private final String description;

        private AutoCloseableLifecycleAction(Class<? extends AutoCloseable> clazz) {
            this.description = new StringBuilder().append("AutoCloseable@").append(System.identityHashCode(this)).append("[").append(clazz.getName()).append(".")
                    .append("close()").append("]").toString();
        }

        @Override
        public void call(Object obj) throws Exception {
            LOG.info("calling action {}", description);
            AutoCloseable.class.cast(obj).close();
        }

        @Override
        public String toString() {
            return description;
        }
    }

}
