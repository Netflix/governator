package com.netflix.governator.lifecycle.warmup;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class Recorder
{
    private final List<String>              recordings = Lists.newArrayList();
    private final List<String>              interruptions = Lists.newArrayList();
    private final RecorderSleepSettings     recorderSleepSettings;

    @Inject
    public Recorder(RecorderSleepSettings recorderSleepSettings)
    {
        this.recorderSleepSettings = recorderSleepSettings;
    }

    public synchronized void        record(String s) throws InterruptedException
    {
        recordings.add(s);

        Long        sleepMs = recorderSleepSettings.getSleepMsFor(s);
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
    }
}
