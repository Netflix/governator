package com.netflix.governator.lifecycle.warmup;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;

@Singleton
public class Recorder
{
    private final List<String>              recordings = Lists.newArrayList();
    private final List<String>              interruptions = Lists.newArrayList();
    private final RecorderSleepSettings     recorderSleepSettings;
    private final Set<Set<String>>          concurrents = Sets.newHashSet();
    private final Set<String>               activeConcurrents = Sets.newHashSet();

    @Inject
    public Recorder(RecorderSleepSettings recorderSleepSettings)
    {
        this.recorderSleepSettings = recorderSleepSettings;
    }

    public synchronized void        record(String s) throws InterruptedException
    {

        recordings.add(s);

        Long        sleepMs = recorderSleepSettings.getSleepMsFor(s);

        activeConcurrents.add(s);
        try
        {
            concurrents.add(ImmutableSet.copyOf(activeConcurrents));
            wait(sleepMs);
        }
        catch ( InterruptedException e )
        {
            interruptions.add(s);
            Thread.currentThread().interrupt();
            throw e;
        }
        finally
        {
            activeConcurrents.remove(s);
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

    public synchronized Set<Set<String>> getConcurrents()
    {
        return ImmutableSet.copyOf(concurrents);
    }
}
