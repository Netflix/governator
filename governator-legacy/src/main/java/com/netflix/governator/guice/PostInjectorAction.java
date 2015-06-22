package com.netflix.governator.guice;

import com.google.inject.Injector;

/**
 * Action to perform after the injector is created.
 * 
 * @author elandau
 */
public interface PostInjectorAction {
    public void call(Injector injector);
}
