package com.netflix.governator.guice.runner.events;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Used mainly for testing the SelfDestructingTerminationEvent will fire the main TerminateEvent
 * after a specified amount of time has elapsed, causing the application to exit.
 * @author elandau
 */
public class SelfDestructingTerminationEvent extends BlockingTerminationEvent {
    public SelfDestructingTerminationEvent(final long timeout, final TimeUnit units) {
        Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).build())
            .schedule(new Runnable() {
                @Override
                public void run() {
                    terminate();
                }
            }, timeout, units);
    }
}
