package com.netflix.governator.guice.runner.standalone;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.netflix.governator.annotations.binding.Main;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.governator.guice.runner.LifecycleRunner;
import com.netflix.governator.guice.runner.TerminateEvent;
import com.netflix.governator.guice.runner.events.BlockingTerminateEvent;
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
        private TerminateEvent terminateEvent;
        
        /**
         * Specify optional command line arguments to be injected.  The arguments can be injected
         * as 
         * 
         * {@code
         *      @Main List<String>
         * }
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
         * the default {@link BlockingTerminateEvent} will be used.
         * @param event
         */
        public Builder withTerminateEvent(TerminateEvent event) {
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
        Injector injector ;
        
        @Inject
        LifecycleManager manager;
        
        @Inject(optional=true)
        @Main Class<?> mainClass;
        
        @Inject(optional=true)
        @Main List<String> args;
        
        @Inject
        @Main TerminateEvent terminateEvent;
        
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
                
                LOG.info("Waiting for terminate event");
                terminateEvent.await();
            } 
            catch (Exception e) {
                LOG.error("Error executing application ", e);
            }
            finally {
                LOG.info("Terminating application");
                manager.close();
            }
        }
    }

    private final List<String> args;
    private final Class<?> main;
    private final TerminateEvent terminateEvent;
    
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

    @Override
    public void configure(BootstrapBinder binder) {
        binder.bind(LifecycleRunner.class).to(StandaloneFramework.class);
        if (main != null) {
            binder.bind(main).in(LazySingletonScope.get());
            binder.bind(new TypeLiteral<Class<?>>() {}).annotatedWith(Main.class).toInstance(main);
        }
        if (args != null) {
            binder.bind(new TypeLiteral<List<String>>() {}).annotatedWith(Main.class).toInstance(args);
        }
        
        if (terminateEvent == null)
            binder.bind(TerminateEvent.class).annotatedWith(Main.class).to(BlockingTerminateEvent.class);
        else 
            binder.bind(TerminateEvent.class).annotatedWith(Main.class).toInstance(terminateEvent);
    }
}
