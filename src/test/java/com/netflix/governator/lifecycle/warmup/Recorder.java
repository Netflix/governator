package com.netflix.governator.lifecycle.warmup;

import com.google.common.collect.Lists;
import java.util.List;

public class Recorder
{
    private final List<String>      recordings = Lists.newArrayList();

    public synchronized void        record(String s)
    {
        recordings.add(s);
    }

    public synchronized List<String>    get()
    {
        return Lists.newArrayList(recordings);
    }

    public synchronized void clear()
    {
        recordings.clear();
    }
}
