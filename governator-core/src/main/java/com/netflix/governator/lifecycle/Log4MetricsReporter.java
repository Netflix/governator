package com.netflix.governator.lifecycle;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;

public class Log4MetricsReporter implements MetricsReporter {
    private static final Logger LOG = LoggerFactory.getLogger(Log4MetricsReporter.class);
    
    @Override
    public <T> void noteConstructTime(Key<T> key, long duration, TimeUnit units) {
        LOG.info("Constructed {} in {} {}", new Object[]{
                key.getTypeLiteral().toString(), 
                TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS), 
                TimeUnit.MILLISECONDS});
    }
}
