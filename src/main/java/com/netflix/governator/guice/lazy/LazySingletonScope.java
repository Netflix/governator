package com.netflix.governator.guice.lazy;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

/**
 * A singleton factory that returns a Guice {@link Scope} that enables lazy singletons
 */
public class LazySingletonScope
{
    /**
     * Returns the scope
     * @return scope
     */
    public static Scope get()
    {
        return instance;
    }

    private static final Scope instance = new LazySingletonScopeImpl();

    private LazySingletonScope()
    {
    }
}
