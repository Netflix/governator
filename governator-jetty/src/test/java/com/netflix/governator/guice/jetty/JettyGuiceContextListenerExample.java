package com.netflix.governator.guice.jetty;

import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.ShutdownHookModule;

public class JettyGuiceContextListenerExample {
    public static void main(String args[]) throws Exception {
        InjectorBuilder.fromModules(
                new SampleServletModule(), 
                new ShutdownHookModule(), 
                new JettyModule())
            .createInjector()
            .awaitTermination();
    }
}
