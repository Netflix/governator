package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.WarmUp;

public class DagInterface {
      /*
        3 Classes all with warmups

            B
          <
        A
          <
            C
     */

  public interface A {
  }

  public interface B {
  }

  public interface C {
  }

  @SuppressWarnings("UnusedParameters")
  @Singleton
  public static class AImpl implements A {
    private final Recorder recorder;

    @Inject
    public AImpl(Recorder recorder, B b, C c) {
      this.recorder = recorder;
    }

    @WarmUp
    public void warmUp() throws InterruptedException {
      recorder.record("A");
    }
  }

  @Singleton
  public static class BImpl implements B {
    private final Recorder recorder;

    @Inject
    public BImpl(Recorder recorder) {
      this.recorder = recorder;
    }

    @WarmUp
    public void warmUp() throws InterruptedException {
      recorder.record("B");
    }
  }

  @Singleton
  public static class CImpl implements C {
    private final Recorder recorder;

    @Inject
    public CImpl(Recorder recorder) {
      this.recorder = recorder;
    }

    @WarmUp
    public void warmUp() throws InterruptedException {
      recorder.record("C");
    }
  }

}