package com.netflix.governator.inject.guice;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

class LazySingletonScope
{
    static Scope get()
    {
        return instance;
    }

    /**
     * A singleton that will never be eager, in contrast to
     * {@link Scopes#SINGLETON}, which Guice eagerly creates sometimes.
     */
    private static final Scope instance = new Scope()
    {
        public<T> Provider<T> scope(Key<T> key, Provider<T> creator)
        {
            return Scopes.SINGLETON.scope(key, creator);
        }

        @Override
        public String toString()
        {
            return "LazySingletonScope";
        }
    };

    private LazySingletonScope()
    {
    }
}
