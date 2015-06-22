package com.netflix.governator;

import java.lang.reflect.Modifier;

import com.google.inject.AbstractModule;

/**
 * Base module that ensures only one module is used when multiple modules
 * are installed using the concrete module class as the dedup key.  To 
 * ensure 'best practices' this class also forces the concrete module to
 * be final.  This is done to prevent the use of inheritance for overriding
 * behavior in favor of using Modules.override().
 * 
 * @author elandau
 *
 */
public abstract class SingletonModule extends AbstractModule {
    public SingletonModule() {
        if (!Modifier.isFinal(getClass().getModifiers())) {
            throw new RuntimeException("Module " + getClass().getName() + " must be final");
        }
    }

    @Override
    protected void configure() {
    }
    
    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }

    @Override
    public String toString() {
      return getClass().getName();
    }
}
