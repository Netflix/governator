package com.netflix.governator.guice.runner.events;

import javax.inject.Singleton;

import com.netflix.governator.guice.runner.TerminationEvent;

/**
 * Simple TerminatEvent using a countdown latch as the termination signal.
 * 
 * @author elandau
 */
@Singleton
public class BlockingTerminationEvent implements TerminationEvent {
    private volatile boolean isTerminated = false;
    
    @Override
    public synchronized void await() throws InterruptedException {
        while (!isTerminated) {
            this.wait();
        }
    }

    @Override
    public synchronized void terminate() {
        isTerminated = true;
        this.notifyAll();
    }

    @Override
    public boolean isTerminated() {
        return isTerminated;
    }
}
