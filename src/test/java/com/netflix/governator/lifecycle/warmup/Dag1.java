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

    public static class A
    {
        @Inject
        public A(B b, C c)
        {
        }

        @WarmUp
        public void warmUp()
        {
            System.out.println("A warmup");
        }
    }

    public static class B
    {
        @Inject
        public B()
        {
        }

        @WarmUp
        public void warmUp()
        {
            System.out.println("B warmup");
        }
    }

    public static class C
    {
        @Inject
        public C()
        {
        }

        @WarmUp
        public void warmUp()
        {
            System.out.println("C warmup");
        }
    }
}
