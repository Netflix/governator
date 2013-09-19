package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.WarmUp;

public class DagNoInject
{
    public static Recorder recorder;

    @SuppressWarnings("UnusedParameters")
    @Singleton
    public static class A
    {
        @Inject
        public A(B b, C c)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("A");
        }
    }

    @Singleton
    public static class B
    {
        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B");
        }
    }

    @Singleton
    public static class C
    {
        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("C");
        }
    }
}
