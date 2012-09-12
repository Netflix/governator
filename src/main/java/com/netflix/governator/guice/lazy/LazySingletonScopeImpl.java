package com.netflix.governator.guice.lazy;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

/**
 * A Guice {@link Scope} that enables lazy singletons
 */
final class LazySingletonScopeImpl implements Scope
{
    @Override
    public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped)
    {
        return Scopes.SINGLETON.scope(key, unscoped);
    }
}
