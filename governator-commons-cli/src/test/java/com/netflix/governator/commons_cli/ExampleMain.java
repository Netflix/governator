package com.netflix.governator.commons_cli;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.inject.AbstractModule;
import com.netflix.governator.commons_cli.modules.OptionsModule;
import com.netflix.governator.guice.annotations.GovernatorConfiguration;

public class ExampleMain {
    @GovernatorConfiguration
    public static class MyBootstrap extends AbstractModule {

        @Override
        protected void configure() {
            bind(ExampleMain.class).asEagerSingleton();
            
            install(new OptionsModule() {
                @Override
                protected void configureOptions() {
                    option('f').hasArgs();
                }
            });
        }

    }
    
    public static void main(final String args[]) {
        Cli.start(MyBootstrap.class, args);
    }

    @Inject
    public ExampleMain(@Named("f") String filename) {
        System.out.println("filename=" + filename);
    }
    
    @PostConstruct
    public void initialize() {
        System.out.println("Application starting");
    }
    
    @PostConstruct
    public void shutdown() {
        System.out.println("Application stopping");
    }

}
