package com.netflix.governator.lifecycle;

import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.lifecycle.warmup.WarmUpException;

/**
 * Arguments used by {@link LifecycleManager} after {@link LifecycleManager#start()} has been called.
 * Bind an instance to this interface in the {@link BootstrapBinder} to override defaults.
 */
public interface PostStartArguments
{
    /**
     * Called when there was a warm up error
     */
    public interface WarmUpErrorHandler
    {
        /**
         * Handle a warm up error. Default behavior is to log and call {@link System#exit(int)}
         *
         * @param exception the error container
         */
        public void warmUpError(WarmUpException exception);
    }

    /**
     * Return the wam up error handler
     *
     * @return error handler
     */
    public WarmUpErrorHandler       getWarmUpErrorHandler();

    /**
     * When performing warm ups after {@link LifecycleManager#start()} has been called
     * the warm up processor will wait a given period of time (default is 3 seconds) so
     * that any other warm up methods can be called together.
     *
     * @return time in milliseconds to wait (default is 3 seconds)
     */
    public long                     getWarmUpPaddingMs();
}
