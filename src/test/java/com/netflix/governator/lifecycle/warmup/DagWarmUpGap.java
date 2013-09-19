package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.WarmUp;

public class DagWarmUpGap
{
    public interface A
    {
    }

    public interface B
    {
    }

    public interface C
    {
    }

    public interface D
    {
    }

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

    @Singleton
    public static class BImpl implements B
    {
        private final Recorder recorder;

        @Inject
        public BImpl(Recorder recorder, C c)
        {
            this.recorder = recorder;
        }
    }

    @Singleton
    public static class CImpl implements C
    {
        private final Recorder recorder;

        @Inject
        public CImpl(Recorder recorder, D c)
        {
            this.recorder = recorder;
        }
    }

    @Singleton
    public static class DImpl implements D
    {
        private final Recorder recorder;

        @Inject
        public DImpl(Recorder recorder)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("D");
        }
    }
}
