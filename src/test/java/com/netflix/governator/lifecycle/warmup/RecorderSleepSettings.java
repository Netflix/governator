package com.netflix.governator.lifecycle.warmup;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class RecorderSleepSettings
{
    private final AtomicLong baseSleepMs = new AtomicLong(DEFAULT_SLEEP_MS);
    private final Map<String, Long> baseSleepMsForString = Maps.newHashMap();

    private static final int    DEFAULT_SLEEP_MS = 5;

    public void setBaseSleep(long time, TimeUnit unit)
    {
        baseSleepMs.set(unit.toMillis(time));
    }

    public void setBaseSleepFor(String s, long time, TimeUnit unit)
    {
        baseSleepMsForString.put(s, unit.toMillis(time));
    }

    public Long getSleepMsFor(String s)
    {
        return baseSleepMsForString.containsKey(s) ? baseSleepMsForString.get(s) : baseSleepMs.get();
    }
}
