package com.netflix.governator.guice.jetty;

import com.netflix.governator.guice.lazy.FineGrainedLazySingleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

@FineGrainedLazySingleton
public class SomeFineGrainedLazySingleton {
    private static final Logger LOG = LoggerFactory.getLogger(SomeFineGrainedLazySingleton.class);
    
    @Inject
    public SomeFineGrainedLazySingleton() {
        LOG.info("@FineGrainedLazySingleton created");
    }
    
    @PreDestroy
    private void shutdown() {
        LOG.info("@FineGrainedLazySingleton#shutdown()");
    }
    
    
}
