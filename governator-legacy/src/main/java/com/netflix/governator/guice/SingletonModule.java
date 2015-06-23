package com.netflix.governator.guice;


/**
 * Base module that ensures only one module is used when multiple modules
 * are installed using the concrete module class as the dedup key.  To 
 * ensure 'best practices' this class also forces the concrete module to
 * be final.  This is done to prevent the use of inheritance for overriding
 * behavior in favor of using Modules.override().
 * 
 * @author elandau
 *
 * @deprecated Use com.netflix.governator.SingletonModule instead
 */
@Deprecated
public abstract class SingletonModule extends com.netflix.governator.SingletonModule {

}
