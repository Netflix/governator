/*
 * Copyright 2012 Netflix, Inc.
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

package com.netflix.governator.guice.mocks;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.lifecycle.AutoBindSingletonMode;
import org.testng.Assert;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicInteger;

@AutoBindSingleton(AutoBindSingletonMode.EAGER)
@RequiredAsset("test")
public class SimpleEagerSingleton
{
    public final AtomicInteger  startCount = new AtomicInteger(0);
    public final AtomicInteger  finishCount = new AtomicInteger(0);

    @Inject
    public SimpleEagerSingleton()
    {
        // NOP
    }

    @PostConstruct
    public void     start() throws InterruptedException
    {
        Assert.assertTrue(SimpleAssetLoader.loadAssetCount.get() > 0);
        startCount.incrementAndGet();
    }

    @PreDestroy
    public void     finish() throws InterruptedException
    {
        finishCount.incrementAndGet();
    }
}
