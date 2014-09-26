package com.netflix.governator.guice.main;

import com.google.inject.AbstractModule;
import com.google.inject.ProvisionException;
import com.netflix.governator.guice.LifecycleInjector;

/**
 * Main class for loading a bootstrap configuration via main().  When running an application set
 * this to the main class and set the first argument to the name of the bootstrap'd class.
 * 
 * java BootstrapMain com.org.MyApplicationBootstrap ...
 * 
 * Where,
 * 
 * <pre>
 * {@code
 *  @GovernatorConfiguration
 *  public class MyApplicationBootstrap extends AbstractModule {
 *     public void configure() {
 *        // your application bindings here
 *     }
 *  }
 * }
 * </pre>
 * 
 * Note that any component in your application can gain access to the command line arguments by injecting
 * Arguments.  Also, it is the responsibility of your application to parse the command line and manage
 * the application lifecycle.  In the future there may be governator subprojects for various cli parsing
 * and command line processing libraries (such as apache commons cli)
 * 
 * <pre>
 * {@code
 * @Singleton
 * public class MyApplication {
 *    @Inject
 *    MyApplication(Arguments args) {
 *    }
 * }
 * }
 * </pre>
 * @author elandau
 */
public class BootstrapMain {
    public static void main(final String args[]) {
        try {
            Class<?> mainClass = Class.forName(args[0]);
            LifecycleInjector.bootstrap(mainClass, new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Arguments.class).toInstance(new Arguments(args));
                }
            });
        } catch (Exception e) {
            throw new ProvisionException("Error instantiating main class", e);
        }
    }
}
