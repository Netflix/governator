package com.netflix.governator.commons_cli;

import com.google.inject.AbstractModule;
import com.google.inject.ProvisionException;
import com.netflix.governator.annotations.binding.Main;
import com.netflix.governator.guice.LifecycleInjector;

public class Cli {
    /**
     * Utility method to start the CommonsCli using a main class and command line arguments
     * 
     * @param mainClass
     * @param args
     */
    public static void start(Class<?> mainClass, final String[] args) {
        try {
            LifecycleInjector.bootstrap(mainClass, new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String[].class).annotatedWith(Main.class).toInstance(args);
                }
            });
        } catch (Exception e) {
            throw new ProvisionException("Error instantiating main class", e);
        }
    }
}
