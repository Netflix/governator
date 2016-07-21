package com.google.inject.internal;

import java.lang.ref.WeakReference;

import com.google.inject.Injector;

/**
 * A hack to provide a reference to the Injector to the {@link SingletonScope} for some
 * edge cases in which it was not properly being initialized. 
 */
public class SingletonScopeInjectorProvider {
    
    public static void setCurrentInjector(Injector injector) {
        if(injector != null && SingletonScope.currentInjector.get() == null && injector instanceof InjectorImpl ) {
            SingletonScope.currentInjector.set(new WeakReference<InjectorImpl>((InjectorImpl) injector));
        }
    }
}
