package com.netflix.governator.guice.jetty;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.governator.LifecycleShutdownSignal;

@Path("/")
@Singleton
public class SampleResource {
    private static final Logger LOG = LoggerFactory.getLogger(SampleResource.class);
    
    private AtomicInteger counter = new AtomicInteger();
    private final LifecycleShutdownSignal event;
    private final AtomicInteger postConstruct = new AtomicInteger();
    private final AtomicInteger preDestroy = new AtomicInteger();
    
    @Inject
    public SampleResource(LifecycleShutdownSignal event) {
        this.event = event;
    }
    
    @GET
    public String getHello() {
        LOG.info("Saying hello");
        return "hello " + counter.incrementAndGet();
    }
    
    @PostConstruct
    public void init() {
        postConstruct.incrementAndGet();
        LOG.info("Post construct " + postConstruct.get());
    }
    
    @PreDestroy
    public void shutdown() {
        preDestroy.incrementAndGet();
        LOG.info("Pre destroy " + preDestroy.get());
    }
    
    
    @Path("kill")
    public String kill() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                event.signal();
            }
        });
        t.setDaemon(true);
        t.start();
        return "killing";
    }
    
    public int getPreDestroyCount() {
        return preDestroy.get();
    }
    
    public int getPostConstructCount() {
        return postConstruct.get();
    }
}
