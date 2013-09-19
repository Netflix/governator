package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.WarmUp;

public class DagNoInjectInterface
{
    public static Recorder recorder;

    public interface A{}
    public interface B{}
    public interface C{}

    @SuppressWarnings("UnusedParameters")
    @Singleton
    public static class AImpl implements A
    {
        @Inject
        public AImpl(B b, C c)
        {
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
        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B");
        }
    }

    @Singleton
    public static class CImpl implements C
    {
        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("C");
        }
    }

}
