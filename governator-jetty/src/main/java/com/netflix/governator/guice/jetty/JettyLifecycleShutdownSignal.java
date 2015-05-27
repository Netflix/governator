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
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    shutdown();
                    server.stop();
                    LOG.info("Jetty Server on port {} shut down complete", port);
                } catch (Exception e) {
                    LOG.warn("Jetty Server on port {} failed to shut down", port, e);
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void await() throws InterruptedException {
        int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        LOG.info("Joining Jetty server on port {}", port);
        server.join();
    }

}
