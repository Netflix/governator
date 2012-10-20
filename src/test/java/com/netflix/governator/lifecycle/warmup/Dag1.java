package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.netflix.governator.annotations.WarmUp;

public class Dag1
{
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
        private final Recorder recorder;

        @Inject
        public A(Recorder recorder, B b, C c)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("A");
        }
    }

    public static class B
    {
        private final Recorder recorder;

        @Inject
        public B(Recorder recorder)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B");
        }
    }

    public static class C
    {
        private final Recorder recorder;

        @Inject
        public C(Recorder recorder)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("C");
        }
    }
}
