package com.netflix.governator.guice.jetty;

import com.netflix.governator.Governator;
import com.netflix.governator.ShutdownHookModule;
import com.netflix.governator.guice.jetty.JettyModule;

public class JettyGuiceContextListenerExample {
    public static void main(String args[]) throws Exception {
        new Governator()
            .addModules(
                new SampleServletModule(), 
                new ShutdownHookModule(), 
                new JettyModule())
            .run()
            .awaitTermination();
    }
}
