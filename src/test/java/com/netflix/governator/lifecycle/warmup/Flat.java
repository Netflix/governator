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

import com.google.inject.Singleton;
import com.netflix.governator.annotations.WarmUp;

public class Flat
{
    /*
        Root classes without dependencies
     */

    @Singleton
    public static class A
    {
        public volatile Recorder recorder;

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("A");
        }
    }

    @Singleton
    public static class B
    {
        public volatile Recorder recorder;

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("B");
        }
    }
}
