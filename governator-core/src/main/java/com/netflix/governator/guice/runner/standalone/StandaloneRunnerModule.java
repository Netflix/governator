package com.netflix.governator.guice.runner.standalone;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.netflix.governator.annotations.binding.Main;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.governator.guice.runner.LifecycleRunner;
import com.netflix.governator.guice.runner.TerminationEvent;
import com.netflix.governator.guice.runner.events.BlockingTerminationEvent;
import com.netflix.governator.lifecycle.LifecycleManager;

/**
 * Implementation of a Runner module that should be used for runtime applications.
 *
 * @author elandau
 */
public class StandaloneRunnerModule implements BootstrapModule {
    private static Logger LOG = LoggerFactory.getLogger(StandaloneRunnerModule.class);

    /**
     * This builder simplifies creation of the module in main()
     */
    public static class Builder {
        private List<String> args = Lists.newArrayList();
        private Class<?> main;
        private TerminationEvent terminateEvent;

        /**
         * Specify optional command line arguments to be injected.  The arguments can be injected
         * as
         *
         * <code>
         *      &#64;Main List<String>
         * </code>
         * @param args
         */
        public Builder withArgs(String[] args) {
            this.args.addAll(Lists.newArrayList(args));
            return this;
        }

        /**
         * Specify an optional main class to instantiate.  Alternatively the
         * main class can be added as an eager singleton
         * @param main
         */
        public Builder withMainClass(Class<?> main) {
            this.main = main;
            return this;
        }

        /**
         * Specify an externally provided {@link TerminationEvent}.  If not specified
         * the default {@link BlockingTerminationEvent} will be used.
         * @param event
         */
        public Builder withTerminateEvent(TerminationEvent event) {
            this.terminateEvent = event;
            return this;
        }

        public StandaloneRunnerModule build() {
            return new StandaloneRunnerModule(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @LazySingleton
    public static class StandaloneFramework implements LifecycleRunner {
        @Inject
        private Injector injector ;

        @Inject
        private LifecycleManager manager;

        @Inject(optional=true)
        private @Main Class<?> mainClass;

        @Inject(optional=true)
        private @Main List<String> args;

        @Inject
        private @Main TerminationEvent terminateEvent;

        /**
         * This is the application's main 'run' loop. which blocks on the termination event
         */
        @PostConstruct
        public void init() {
            try {
                LOG.info("Starting application");
                manager.start();

                if (mainClass != null)
                    injector.getInstance(mainClass);

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
            }
            catch (Exception e) {
                LOG.error("Error executing application ", e);
            }
        }
    }

    private final List<String> args;
    private final Class<?> main;
    private final TerminationEvent terminateEvent;

    public StandaloneRunnerModule(String[] args, Class<?> main) {
        this.args = ImmutableList.copyOf(args);
        this.main = main;
        this.terminateEvent = null;
    }

    private StandaloneRunnerModule(Builder builder) {
        this.args = builder.args;
        this.main = builder.main;
        this.terminateEvent = builder.terminateEvent;
    }

    @Singleton
    public static class MainInjectorModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(LifecycleRunner.class).to(StandaloneFramework.class).asEagerSingleton();
        }
    }

    @Override
    public void configure(BootstrapBinder binder) {
        binder.bind(MainInjectorModule.class);

        if (main != null) {
            binder.bind(main).in(LazySingletonScope.get());
            binder.bind(new TypeLiteral<Class<?>>() {}).annotatedWith(Main.class).toInstance(main);
        }
        if (args != null) {
            binder.bind(new TypeLiteral<List<String>>() {}).annotatedWith(Main.class).toInstance(args);
        }

        if (terminateEvent == null)
            binder.bind(TerminationEvent.class).annotatedWith(Main.class).to(BlockingTerminationEvent.class);
        else
            binder.bind(TerminationEvent.class).annotatedWith(Main.class).toInstance(terminateEvent);
    }
}
