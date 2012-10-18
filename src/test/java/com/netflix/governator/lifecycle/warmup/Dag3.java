package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.netflix.governator.annotations.WarmUp;

public class Dag3
{
    public static final Recorder recorder = new Recorder();

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
    public static class A
    {
        @Inject
        public A(BnW bnw, B b, D d)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("A");
            Thread.sleep((int)(5 * Math.random()));
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class B
    {
        @Inject
        public B(CnW cnw)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B");
            Thread.sleep((int)(5 * Math.random()));
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class BnW
    {
        @Inject
        public BnW(C c)
        {
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class C
    {
        @Inject
        public C(D d)
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("C");
            Thread.sleep((int)(5 * Math.random()));
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class CnW
    {
        @Inject
        public CnW(D d)
        {
        }
    }

    public static class D
    {
        @Inject
        public D()
        {
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("D");
            Thread.sleep((int)(5 * Math.random()));
        }
    }
}
