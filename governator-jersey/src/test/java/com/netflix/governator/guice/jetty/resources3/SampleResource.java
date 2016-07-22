package com.netflix.governator.guice.jetty.resources3;

import com.netflix.governator.LifecycleShutdownSignal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
@Singleton
public class SampleResource {
    private static final Logger LOG = LoggerFactory.getLogger(SampleResource.class);
    
    private AtomicInteger counter = new AtomicInteger();
    
    @Inject
    public SampleResource(LifecycleShutdownSignal event) {
    }
    
    @GET
    public String getHello() {
        LOG.info("Saying hello");
        return "hello " + counter.incrementAndGet();
    }
}
