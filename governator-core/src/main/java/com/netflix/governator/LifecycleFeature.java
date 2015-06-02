package com.netflix.governator;

import java.util.List;

/**
 * Each LifecycleFeature provides support for specific post constructor 
 * pre-@PostConstruct processing of an injected object.  For example,
 * {@link ConfigurationLifecycleFeature} enables configuration mapping
 * prior to @PostConstruct being called.
 * 
 * {@link LifecycleFeature}s are added via a multibinding. For example,
 * 
 * <pre>
 * {@code
 * Multibinder.newSetBinder(binder(), LifecycleFeature.class).addBinding().to(ConfigurationLifecycleFeature.class);
 * }
 * </pre>
 * 
 * @author elandau
 */
public interface LifecycleFeature {
    /**
     * Return a list of actions to perform on object of this type as part of 
     * lifecycle processing.  Each LifecycleAction will likely be tied to processing 
     * of a specific field or method.
     * 
     * @param type
     * @return
     */
    List<LifecycleAction> getActionsForType(Class<?> type);
}
