package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.WarmUp;

public class DagCircular
{
    public interface A{}

    @SuppressWarnings("UnusedParameters")
    @Singleton
    public static class AImpl implements A
    {
        private final Recorder recorder;

        @Inject
        public AImpl(Recorder recorder, B b)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("A");
        }
    }

    public interface B {}

    @SuppressWarnings("UnusedParameters")
    @Singleton
    public static class BImpl implements B
    {
        private final Recorder recorder;

        @Inject
        public BImpl(Recorder recorder, A a)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B");
        }
    }
}
