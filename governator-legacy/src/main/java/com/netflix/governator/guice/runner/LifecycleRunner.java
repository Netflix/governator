package com.netflix.governator.guice.runner;

import com.netflix.governator.guice.runner.standalone.StandaloneRunnerModule;

/**
 * Abstraction defining the application runtime framework for an application using
 * Governator.  If a binding for ApplicationFramework exists Governator will
 * create the instance of the ApplicationFramework immediately after creating
 * the bootstrap module.  It is the application framework's responsibility
 * to call {@link com.netflix.governator.lifecycle.LifecycleManager LifecycleManager}
 * start and stop as well as manage the application termination mechanism.
 *
 * A {@link StandaloneRunnerModule} is provided for simple command line
 * applications.
 *
 * Additional LifecycleRunner implementations may be provided for running
 * Jetty, Karyon, etc...
 *
 * @author elandau
 *
 */
public interface LifecycleRunner {
}
