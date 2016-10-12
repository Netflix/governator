package com.netflix.governator.lifecycle;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

public class LocalScope implements Scope {

    final ThreadLocal<Map<Key<?>, Object>> content = new ThreadLocal<Map<Key<?>, Object>>();

    public void enter() {
        checkState(content.get() == null, "LocalScope already exists in thread %s", Thread.currentThread());
        content.set(Maps.<Key<?>, Object> newHashMap());           
    }

    public void exit() {
        Map<Key<?>, Object> scopeContents = content.get();
        checkState(scopeContents != null, "No LocalScope found in thread %s", Thread.currentThread());
        scopeContents.clear();
        content.remove();
    }
    
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {
            public T get() {
                Map<Key<?>, Object> scopedObjects = content.get();
                if (scopedObjects == null) {
                    throw new OutOfScopeException("No LocalScope found in thread " + Thread.currentThread());
                }

                @SuppressWarnings("unchecked")
                T current = (T) scopedObjects.get(key);
                if (current == null) {
                    current = unscoped.get();

                    // don't remember proxies; these exist only to serve
                    // circular dependencies
                    if (Scopes.isCircularProxy(current)) {
                        return current;
                    }

                    scopedObjects.put(key, current);
                }
                return current;
            }
        };
    }
}