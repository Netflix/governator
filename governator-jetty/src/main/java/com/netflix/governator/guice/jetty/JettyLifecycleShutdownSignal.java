package com.netflix.governator.guice.jetty;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.governator.AbstractLifecycleShutdownSignal;
import com.netflix.governator.LifecycleManager;

@Singleton
public class JettyLifecycleShutdownSignal extends AbstractLifecycleShutdownSignal {
    private static final Logger LOG = LoggerFactory.getLogger(JettyLifecycleShutdownSignal.class);
    
    private final Server server;
    
    @Inject
    public JettyLifecycleShutdownSignal(Server server, LifecycleManager manager) {
        super(manager);
        this.server = server;
    }

    @Override
    public void signal() {
        final int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        LOG.info("Jetty Server on port {} shutting down", port);
        try {
            shutdown();
            server.stop();
        } catch (Exception e) {
            LOG.error("Failed to shut down jetty on port{}", port, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void await() throws InterruptedException {
        int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        LOG.info("Joining Jetty server on port {}", port);
        server.join();
    }

}
