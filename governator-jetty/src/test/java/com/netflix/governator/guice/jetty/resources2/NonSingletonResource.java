package com.netflix.governator.guice.jetty.resources2;

import com.netflix.governator.guice.jetty.SomeFineGrainedLazySingleton;
import com.netflix.governator.guice.jetty.SomeLazySingleton;
import com.netflix.governator.guice.jetty.SomeSingleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class NonSingletonResource {
    private static final Logger LOG = LoggerFactory.getLogger(NonSingletonResource.class);
    
    @Inject
    public NonSingletonResource(SomeLazySingleton lazySingleton, SomeSingleton singleton, SomeFineGrainedLazySingleton fglSingleton) {
        LOG.info("NonSingletonResource()");
    }
    
    @PreDestroy
    private void shutdown() {
        LOG.info("NonSingletonResource#shutdown()");
    }
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String echo() {
        LOG.info("Saying hello");
        return "hello";
    }
}