package com.netflix.governator.guice;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.rules.ExternalResource;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.netflix.governator.lifecycle.LifecycleManager;

/**
 * Rule for testing with governator.  The rule provides the following conveniences
 * 1.  Mechanism to customize the configuration via external modules.
 * 2.  Auto shutdown the lifecycle manager when a test ends
 * 3.  Methods to test whether certain bindings were heard and injected
 *
 * Usage
 *
 * <pre>
 * public class MyUnitTest {
 *     &#64;Rule
 *     public LifecycleTester tester = new LifecycleTester(new MyTestSuite());
 *
 *     &#64;Test
 *     public void test() {
 *         // Test specific setup
 *         tester.builder().
 *             withAdditionalModule(new TheModuleImTesting());
 *
 *         // Creates the injector and start LifecycleManager
 *         tester.start();
 *
 *         // Your test code goes here
 *
 *     } // On termination the LifecycleTester will shutdown LifecycleManager
 * }
 *
 * </pre>
 *
 * public static class
 * @author elandau
 */
public class LifecycleTester extends ExternalResource {
    private BootstrapModule[] suites;
    private Injector injector;
    private Class<?> bootstrap;
    private LifecycleInjectorBuilder builder;
    private Module externalModule;
    
    public LifecycleTester(List<BootstrapModule> suites) {
        this.suites = suites.toArray(new BootstrapModule[suites.size()]);
    }

    public LifecycleTester(BootstrapModule ... suites) {
        this.suites = suites;
    }
    
    public LifecycleTester(Class bootstrap, BootstrapModule ... suites) {
        this.bootstrap = bootstrap;
        this.suites = suites;
    }
    
    /**
     *
     * @return the new Injector
     */
    public Injector start() {
        if (bootstrap != null) {
            injector = LifecycleInjector.bootstrap(bootstrap, externalModule, suites);
        }
        else {
            builder = LifecycleInjector.builder();
            injector = builder.build().createInjector();
        }
        LifecycleManager manager = injector.getInstance(LifecycleManager.class);
        try {
            manager.start();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        return injector;
    }

    public LifecycleTester withBootstrapModule(BootstrapModule bootstrapModule) {
        if (this.suites == null || this.suites.length == 0) {
            this.suites = new BootstrapModule[] { bootstrapModule };
        }
        else {
            this.suites = Arrays.copyOf(this.suites, this.suites.length + 1);
            this.suites[this.suites.length-1] = bootstrapModule;
        }
        return this;
    }
    
    public LifecycleTester withExternalBindings(Module module) {
        this.externalModule = module;
        return this;
    }
    public LifecycleInjectorBuilder builder() {
        return builder;
    }

    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    public <T> T getInstance(Key<T> type) {
        return injector.getInstance(type);
    }

    public <T> T getInstance(TypeLiteral<T> type) {
        return injector.getInstance(Key.get(type));
    }

    /**
     * Override to tear down your specific external resource.
     */
    protected void after() {
        if (injector != null) {
            LifecycleManager manager = injector.getInstance(LifecycleManager.class);
            try {
                manager.close();
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }
    }
}
