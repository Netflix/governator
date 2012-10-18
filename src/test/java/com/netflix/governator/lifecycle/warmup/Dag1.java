package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.netflix.governator.annotations.WarmUp;

public class Dag1
{
    public static final Recorder recorder = new Recorder();

    /*
        3 Classes all with warmups

            B
          <
        A
          <
            C
     */

    @SuppressWarnings("UnusedParameters")
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

    public static class B
    {
        @Inject
        public B()
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B");
        }
    }

    public static class C
    {
        @Inject
        public C()
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("C");
        }
    }
}
