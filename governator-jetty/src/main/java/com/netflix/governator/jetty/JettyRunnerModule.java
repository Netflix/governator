package com.netflix.governator.jetty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.runner.LifecycleRunner;
import com.netflix.governator.guice.runner.TerminationEvent;
import com.netflix.governator.lifecycle.LifecycleManager;

/**
 * This runner creates a Jetty web app container and runs until the termination
 * event is fired.
 * 
 * @author elandau
 *
 */
public class JettyRunnerModule implements BootstrapModule {
    private static Logger LOG = LoggerFactory.getLogger(JettyRunnerModule.class);

    private static final int DEFAULT_PORT = 8080;
    private static final String SERVLET_ROOT = "/";

    @Singleton
    public static class JettyRunner implements LifecycleRunner {
        @Inject
        private Injector injector;

        @Inject
        private LifecycleManager manager;

        @Inject
        private Configuration config;

        @Inject
        private Server server;

        @Inject
        private TerminationEvent terminateEvent;
        
        @PostConstruct
        public void init() {
            try {
                manager.start();

                // Create the server.
                ServletContextHandler sch = new ServletContextHandler(server, config.servletRoot);
                sch.addEventListener(new GuiceServletContextListener() {
                    @Override
                    protected Injector getInjector() {
                        return injector;
                    }
                });

                sch.addFilter(GuiceFilter.class,     config.servletRoot + "*", null);
                sch.addServlet(DefaultServlet.class, config.servletRoot);
                
                // Start the server
                server.start();

                // Wait for termination in a separate thread
                final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("GovernatorStandaloneTerminator-%d").build());
                executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            LOG.info("Waiting for terminate event");
                            try {
                                terminateEvent.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            LOG.info("Terminating application");
                            manager.close();
                            executor.shutdown();
                        }
                    });

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Singleton
    public static class ServerTerminationEvent implements TerminationEvent {
        @Inject
        private Server server;

        @Override
        public void await() throws InterruptedException {
            server.join();
        }

        @Override
        public void terminate() {
            try {
                server.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isTerminated() {
            return server.isStopped();
        }
    }

    @Singleton
    private static class Configuration implements Cloneable {
        int port = DEFAULT_PORT;
        String servletRoot = SERVLET_ROOT;

        public Configuration clone() {
            Configuration config = new Configuration();
            config.port = this.port;
            config.servletRoot = this.servletRoot;
            return config;
        }
    }

    /**
     * This builder simplifies creation of the module in main()
     */
    public static class Builder {
        private Configuration config = new Configuration();

        public Builder withPort(int port) {
            this.config.port = port;
            return this;
        }

        public JettyRunnerModule build() {
            return new JettyRunnerModule(this);
        }
    }

    private final Configuration config;

    public JettyRunnerModule(Builder builder) {
        this.config = builder.config.clone();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Singleton
    public static class MainInjectorModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(LifecycleRunner.class).to(JettyRunner.class)
                    .asEagerSingleton();
            bind(TerminationEvent.class).to(ServerTerminationEvent.class);
        }
    }

    @Override
    public void configure(BootstrapBinder binder) {
        binder.bind(MainInjectorModule.class);
        binder.bind(Configuration.class).toInstance(config);
        binder.bind(Server.class).toInstance(new Server(config.port));

    }
}