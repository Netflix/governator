package com.netflix.governator;

import com.google.inject.AbstractModule;

/**
 * @deprecated This class provides little value and may encourage an unnecessary dependency
 * between libraries and Governator instead of plain Guice.
 */
public class DefaultModule extends AbstractModule {
    @Override
    protected void configure() {
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
