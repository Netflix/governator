package com.netflix.governator.internal;

import java.lang.reflect.Field;
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
import com.netflix.governator.internal.JSR250LifecycleAction.ValidationMode;
import com.netflix.governator.internal.TypeInspector.TypeVisitor;

/**
 * Special LifecycleFeature to support @PreDestroy annotation processing and
 * java.lang.AutoCloseable detection. Note that this feature is implicit in
 * LifecycleModule and therefore does not need to be added using the
 * LifecycleFeature multibinding.
 * 
 * @author elandau
 */
public final class PreDestroyLifecycleFeature implements LifecycleFeature {
    private static final Logger LOG = LoggerFactory.getLogger(PreDestroyLifecycleFeature.class);
    private final ValidationMode validationMode;

    public PreDestroyLifecycleFeature(ValidationMode validationMode) {
        this.validationMode = validationMode;
    }
    
    @Override
    public List<LifecycleAction> getActionsForType(final Class<?> type) {
       return TypeInspector.accept(type, new PreDestroyVisitor());
    }

    @Override
    public String toString() {
        return "PreDestroy";
    }

    private  class PreDestroyVisitor implements TypeVisitor, Supplier<List<LifecycleAction>> {
        private Set<String> visitContext = new HashSet<>();
        private List<LifecycleAction> typeActions = new ArrayList<>();

        @Override
        public boolean visit(final Class<?> clazz) {
            boolean continueVisit = !clazz.isInterface();
            if (continueVisit && AutoCloseable.class.isAssignableFrom(clazz)) {
                AutoCloseableLifecycleAction closeableAction = new AutoCloseableLifecycleAction(
                        clazz.asSubclass(AutoCloseable.class));
                LOG.debug("adding action {}", closeableAction);
                typeActions.add(closeableAction);
                continueVisit = false;
            }
            return continueVisit;
        }

        @Override
        public boolean visit(final Method method) {

            final String methodName = method.getName();
            if (method.isAnnotationPresent(PreDestroy.class)) {
                if (!visitContext.contains(methodName)) {
                    try {
                        LifecycleAction destroyAction = new JSR250LifecycleAction(PreDestroy.class, method, validationMode);
                        LOG.debug("adding action {}", destroyAction);
                        typeActions.add(destroyAction);
                        visitContext.add(methodName);
                    } catch (IllegalArgumentException e) {
                        LOG.info("ignoring @PreDestroy method {}.{}() - {}", method.getDeclaringClass().getName(),
                                methodName, e.getMessage());
                    }
                }
            } else if (method.getReturnType() == Void.TYPE && method.getParameterTypes().length == 0 && !Modifier.isFinal(method.getModifiers())) {
                // method potentially overrides superclass method and annotations
                visitContext.add(methodName);
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
