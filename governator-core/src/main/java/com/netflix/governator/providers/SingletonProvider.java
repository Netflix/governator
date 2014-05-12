package com.netflix.governator.providers;

import javax.inject.Provider;

/**
 * Base class for {@link Providers} that which to enforce singleton semantics
 * for a type.
 * 
 * Note that this class is needed since annotating a Provider with @Singleton
 * makes the Provider a singleton and NOT the type is it providing.  So the
 * same Provider is returned when the Provider is injector the a new call to 
 * get() is made whenever the type T is injected.
 * 
 * @author elandau
 *
 * @param <T>
 */
public abstract class SingletonProvider<T> implements Provider<T> {
    private volatile T obj;
    private Object lock = new Object();
    
    /**
     * Return the caches instance or call the internal {@link create} method
     * when creating the object for the first time.
     * 
     */
    @Override
    public final T get() {
        if (obj == null) {
            synchronized (lock) {
                if (obj == null) {
                    obj = create();
                }
            }
        }
        return obj;
    }
    
    /**
     * Implement the actual object creation here instead of in get()
     * @return
     */
    protected abstract T create();
}
