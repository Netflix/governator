package com.netflix.governator.lifecycle;

import com.google.inject.ImplementedBy;

@ImplementedBy(DefaultLifecycleMethodsFactory.class)
public interface LifecycleMethodsFactory {
    <T> LifecycleMethods create(Class<T> type);
}
