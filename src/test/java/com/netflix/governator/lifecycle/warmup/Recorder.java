package com.netflix.governator.lifecycle.warmup;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Recorder
{
    private final List<String>      recordings = Lists.newArrayList();
    private final List<String>      interruptions = Lists.newArrayList();
    private final AtomicLong        baseSleepMs = new AtomicLong(DEFAULT_SLEEP_MS);
    private final Map<String, Long> baseSleepMsForString = Maps.newHashMap();

    private static final int    DEFAULT_SLEEP_MS = 5;

    public synchronized void        record(String s) throws InterruptedException
    {
        recordings.add(s);

        Long        sleepMs = baseSleepMsForString.containsKey(s) ? baseSleepMsForString.get(s) : baseSleepMs.get();
        try
        {
            wait((long)(sleepMs * Math.random()) + 1);
        }
        catch ( InterruptedException e )
        {
            interruptions.add(s);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public synchronized List<String> getRecordings()
    {
        return Lists.newArrayList(recordings);
    }

    public synchronized List<String>    getInterruptions()
    {
        return Lists.newArrayList(interruptions);
    }

    public synchronized void clear()
    {
        recordings.clear();
        baseSleepMs.set(DEFAULT_SLEEP_MS);
        baseSleepMsForString.clear();
    }

    public void setBaseSleep(long time, TimeUnit unit)
    {
        baseSleepMs.set(unit.toMillis(time));
    }

    public void setBaseSleepFor(String s, long time, TimeUnit unit)
    {
        baseSleepMsForString.put(s, unit.toMillis(time));
    }
}
