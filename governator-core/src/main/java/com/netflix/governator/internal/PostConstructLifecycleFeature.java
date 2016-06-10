package com.netflix.governator.internal;

import java.lang.reflect.Field;
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
import com.netflix.governator.internal.JSR250LifecycleAction.ValidationMode;
import com.netflix.governator.internal.TypeInspector.TypeVisitor;

/**
 * Special LifecycleFeature to support @PostConstruct annotation processing.
 * Note that this feature is implicit in LifecycleModule and therefore does not
 * need to be added using the LifecycleFeature multibinding.
 * 
 * @author elandau
 */
public final class PostConstructLifecycleFeature implements LifecycleFeature {
    private static final Logger LOG = LoggerFactory.getLogger(PostConstructLifecycleFeature.class);
    private final ValidationMode validationMode;

    public PostConstructLifecycleFeature(ValidationMode validationMode) {
        this.validationMode = validationMode;
    }
    
    @Override
    public List<LifecycleAction> getActionsForType(final Class<?> type) {
        return TypeInspector.accept(type, new PostConstructVisitor());
    }

    @Override
    public String toString() {
        return "PostConstruct";
    }

    private class PostConstructVisitor implements TypeVisitor, Supplier<List<LifecycleAction>> {
        private final Set<String> visitContext;
        private final LinkedList<LifecycleAction> typeActions;
        
        public PostConstructVisitor() {
            this.visitContext = new HashSet<>();
            this.typeActions = new LinkedList<>();
        }

        @Override
        public boolean visit(final Class<?> clazz) {
            return !clazz.isInterface();
        }

        @Override
        public boolean visit(final Method method) {
            final String methodName = method.getName();
            if (method.isAnnotationPresent(PostConstruct.class)) {
                if (!visitContext.contains(methodName)) {
                    try {
                        LifecycleAction postConstructAction = new JSR250LifecycleAction(PostConstruct.class, method, validationMode);
                        LOG.debug("adding action {}", postConstructAction);
                        this.typeActions.addFirst(postConstructAction);
                        visitContext.add(methodName);
                    }
                    catch (IllegalArgumentException e) {
                        LOG.info("ignoring @PostConstruct method {}.{}() - {}", method.getDeclaringClass().getName(), methodName, e.getMessage());                        
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

}
