package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.netflix.governator.annotations.WarmUp;

public class Dag2
{
    public static final Recorder recorder = new Recorder();

    /*
        3 tiers of classes all with warmups

             B1
           <    >
        A1        C1
           <    >
             B2
           <    >
        A2        C2
           <    >
             B3
           <    >
        A3        C3
           <    >
             B4
     */

    public static class C1
    {
        @Inject
        public C1()
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("C1");
        }
    }

    public static class C2
    {
        @Inject
        public C2()
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("C2");
        }
    }

    public static class C3
    {
        @Inject
        public C3()
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("C3");
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class B1
    {
        @Inject
        public B1(C1 c1)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B1");
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class B2
    {
        @Inject
        public B2(C1 c1, C2 c2)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B2");
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class B3
    {
        @Inject
        public B3(C2 c2, C3 c3)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B3");
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class B4
    {
        @Inject
        public B4(C3 c3)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B4");
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class A1
    {
        @Inject
        public A1(B1 b1, B2 b2)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("A1");
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class A2
    {
        @Inject
        public A2(B2 b2, B3 b3)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("A2");
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class A3
    {
        @Inject
        public A3(B3 b3, B4 b4)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("A3");
        }
    }
}
