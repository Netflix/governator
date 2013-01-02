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
