package com.netflix.governator.guice.jetty;

import com.netflix.governator.guice.lazy.LazySingleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

@LazySingleton
public class SomeLazySingleton {
    private static final Logger LOG = LoggerFactory.getLogger(SomeLazySingleton.class);
    
    @Inject
    public SomeLazySingleton() {
        LOG.info("SomeLazySingleton created");
    }
    
    @PreDestroy
    private void shutdown() {
        LOG.info("SomeLazySingleton#shutdown()");
    }
    
    
}
