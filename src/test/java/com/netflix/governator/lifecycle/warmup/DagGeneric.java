package com.netflix.governator.lifecycle.warmup;

import javax.inject.Inject;

import com.netflix.governator.annotations.WarmUp;

public class DagGeneric
{
    public interface Supplier<T>{}
    public static class Supplied
    {
        private final Recorder recorder;

        @Inject
        public Supplied(Recorder recorder, Supplier<String> string, Supplier<Integer> integer){
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("Supplied");
        }
    }

    public static class StringSupplier implements Supplier<String>
    {
        private final Recorder recorder;

        @Inject
        public StringSupplier(Recorder recorder)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("StringSupplier");
        }
    }

    public static class IntegerSupplier implements Supplier<Integer>
    {
        private final Recorder recorder;

        @Inject
        public IntegerSupplier(final Recorder recorder, final Supplier<String> string)
        {
            this.recorder = recorder;
        }

        @WarmUp
        public void warmUp() throws InterruptedException
        {
            recorder.record("IntegerSupplier");
        }
    }
}
