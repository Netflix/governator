package com.netflix.governator.guice.jetty;

import com.netflix.governator.guice.lazy.LazySingleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

@LazySingleton
public class SomeSingleton {
    private static final Logger LOG = LoggerFactory.getLogger(SomeSingleton.class);
    
    @Inject
    public SomeSingleton() {
        LOG.info("SomeSingleton created");
    }
    
    @PreDestroy
    private void shutdown() {
        LOG.info("SomeSingleton#shutdown()");
    }
    
    
}
