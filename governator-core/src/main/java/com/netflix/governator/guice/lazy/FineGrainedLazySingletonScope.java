package com.netflix.governator.guice.lazy;

import com.google.inject.Scope;

/**
 * A singleton factory that returns a Guice {@link Scope} that enables fine grained lazy singletons.
 *
 * @see FineGrainedLazySingleton
 * @deprecated Use javax.inject.Singleton instead.  FineGrainedLazySingleton is not needed 
 * as of Guice4 which fixes the global lock issue.
 */
@Deprecated
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
