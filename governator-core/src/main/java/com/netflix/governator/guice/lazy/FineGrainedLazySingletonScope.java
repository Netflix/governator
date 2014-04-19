package com.netflix.governator.guice.lazy;

import com.google.inject.Scope;

/**
 * A singleton factory that returns a Guice {@link Scope} that enables fine grained lazy singletons.
 *
 * @see FineGrainedLazySingleton
 */
public class FineGrainedLazySingletonScope
{
    private static final Scope instance = new FineGrainedLazySingletonScopeImpl();

    /**
     * Returns the scope
     * @return scope
     */
    public static Scope get()
    {
        return instance;
    }
}
