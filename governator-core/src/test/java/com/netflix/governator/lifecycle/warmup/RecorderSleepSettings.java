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

import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class RecorderSleepSettings
{
    private final AtomicLong baseSleepMs = new AtomicLong(DEFAULT_SLEEP_MS);
    private final Map<String, Long> baseSleepMsForString = Maps.newHashMap();
    private final AtomicBoolean randomize = new AtomicBoolean(true);

    private static final int    DEFAULT_SLEEP_MS = 1000;

    public void setBaseSleep(long time, TimeUnit unit)
    {
        baseSleepMs.set(unit.toMillis(time));
    }

    public void setBaseSleepFor(String s, long time, TimeUnit unit)
    {
        baseSleepMsForString.put(s, unit.toMillis(time));
    }

    public long getSleepMsFor(String s)
    {
        long sleepMs = baseSleepMsForString.containsKey(s) ? baseSleepMsForString.get(s) : baseSleepMs.get();
        if ( randomize.get() )
        {
            sleepMs = ((int)(sleepMs * Math.random()) + 1);
        }
        return sleepMs;
    }

    public void setRandomize(boolean randomize)
    {
        this.randomize.set(randomize);
    }
}
