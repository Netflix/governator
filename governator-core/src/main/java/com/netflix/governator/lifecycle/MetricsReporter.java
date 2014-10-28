package com.netflix.governator.lifecycle;

import java.util.concurrent.TimeUnit;

import com.google.inject.ImplementedBy;
import com.google.inject.Key;

/**
 * SPI to receive object construction metrics. 
 * 
 * @author elandau
 */
@ImplementedBy(Log4MetricsReporter.class)
public interface MetricsReporter {
    <T> void noteConstructTime(Key<T> key, long duration, TimeUnit units);
}
