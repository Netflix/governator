package com.netflix.governator.guice;

import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.netflix.governator.annotations.AutoBindSingleton;

/**
 * Controls how {@link AutoBindSingleton}'s are bound
 */
public enum SingletonMode
{
    /**
     * Bind as a lazy singleton. The object will be instantiated only when referenced.
     */
    LAZY()
    {
        @Override
        public Scope getScope()
        {
            return LazySingletonScope.get();
        }
    },

    /**
     * Bind as an eager singleton. The object will always be instantiated.
     */
    EAGER()
    {
        @Override
        public Scope getScope()
        {
            return Scopes.SINGLETON;
        }
    }
    ;

    public abstract Scope getScope();
}
