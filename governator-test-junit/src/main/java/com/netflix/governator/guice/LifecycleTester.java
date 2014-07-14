package com.netflix.governator.guice;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.rules.ExternalResource;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;
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
    private LifecycleInjectorBuilderSuite[] suites;
    private Injector injector;
    private Class<?> bootstrap;
    private LifecycleInjectorBuilder builder;
    
    public LifecycleTester(List<LifecycleInjectorBuilderSuite> suites) {
        this.suites = suites.toArray(new LifecycleInjectorBuilderSuite[suites.size()]);
    }

    public LifecycleTester(LifecycleInjectorBuilderSuite ... suites) {
        this.suites = suites;
    }
    
    public LifecycleTester(Class bootstrap, LifecycleInjectorBuilderSuite ... suites) {
        this.bootstrap = bootstrap;
        this.suites = suites;
    }
    
    /**
     *
     * @return the new Injector
     */
    public Injector start() {
        if (bootstrap != null) {
            injector = LifecycleInjector.bootstrap(bootstrap, suites);
        }
        else {
            builder = LifecycleInjector.builder();
            builder = LifecycleInjector.builder();
            if (suites != null) {
                for (LifecycleInjectorBuilderSuite suite : suites) {
                    suite.configure(builder);
                }
            }
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

    public LifecycleTester withSuite(LifecycleInjectorBuilderSuite suite) {
        if (this.suites == null || this.suites.length == 0) {
            this.suites = new LifecycleInjectorBuilderSuite[] { suite };
        }
        else {
            this.suites = Arrays.copyOf(this.suites, this.suites.length + 1);
            this.suites[this.suites.length-1] = suite;
        }
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
