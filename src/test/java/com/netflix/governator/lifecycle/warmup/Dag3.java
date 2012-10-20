package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.WarmUp;

public class Dag3
{
    /*
        Mix of classes with/without warmups and
        dependencies that cross tiers


                  C
                <      >
            BnW
          <
        A       ==>       D
          <
            B
                <      >
                  CnW
     */

    @SuppressWarnings("UnusedParameters")
    @Singleton
    public static class A
    {
        private final Recorder recorder;

        @Inject
        public A(Recorder recorder, BnW bnw, B b, D d)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("A");
        }
    }

    @SuppressWarnings("UnusedParameters")
    @Singleton
    public static class B
    {
        private final Recorder recorder;

        @Inject
        public B(Recorder recorder, CnW cnw)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B");
        }
    }

    @SuppressWarnings("UnusedParameters")
    @Singleton
    public static class BnW
    {
        @Inject
        public BnW(C c)
        {
        }
    }

    @SuppressWarnings("UnusedParameters")
    @Singleton
    public static class C
    {
        private final Recorder recorder;

        @Inject
        public C(Recorder recorder, D d)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("C");
        }
    }

    @SuppressWarnings("UnusedParameters")
    @Singleton
    public static class CnW
    {
        @Inject
        public CnW(D d)
        {
        }
    }

    @Singleton
    public static class D
    {
        private final Recorder recorder;

        @Inject
        public D(Recorder recorder)
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
