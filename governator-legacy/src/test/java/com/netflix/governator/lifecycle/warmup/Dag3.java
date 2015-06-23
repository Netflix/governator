/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
