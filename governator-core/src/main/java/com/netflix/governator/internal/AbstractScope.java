package com.netflix.governator.internal;

import com.google.inject.Scope;

public abstract class AbstractScope implements Scope {
    /**
     * @return Return true if this scope uses Guice's Scopes.SINGLETON as its underlying scope
     */
    public boolean isSingletonScope() {
        return false;
    }
}
