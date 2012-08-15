package com.netflix.governator.lifecycle;

import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.guice.LazySingletonScope;

/**
 * Controls how {@link AutoBindSingleton}'s are bound
 */
public enum AutoBindSingletonMode
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
