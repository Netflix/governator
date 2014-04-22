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
    @Singleton
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

    @Singleton
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

    @Singleton
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
