package com.netflix.governator.guice.runner.events;

import java.util.concurrent.CountDownLatch;

import javax.inject.Singleton;

import com.netflix.governator.guice.runner.TerminateEvent;

/**
 * Simple TerminatEvent using a countdown latch as the termination signal.
 * 
 * @author elandau
 */
@Singleton
public class BlockingTerminateEvent implements TerminateEvent {

    private CountDownLatch latch = new CountDownLatch(1);
    
    @Override
    public void await() throws InterruptedException {
        latch.await();
    }

    @Override
    public void set() {
        latch.countDown();
    }

    @Override
    public boolean isSet() {
        return latch.getCount() == 0;
    }
}
