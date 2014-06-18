package com.netflix.governator.guice;

import java.util.List;

import org.junit.Assert;
import org.junit.rules.ExternalResource;

import com.google.common.collect.Lists;
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
    private LifecycleInjectorBuilder builder;
    private Injector injector;

    public LifecycleTester(List<LifecycleInjectorBuilderSuite> suites) {
        builder = LifecycleInjector.builder();
        if (suites != null) {
            for (LifecycleInjectorBuilderSuite suite : suites) {
                suite.configure(builder);
            }
        }
    }

    public LifecycleTester(LifecycleInjectorBuilderSuite ... suites) {
        this(Lists.newArrayList(suites));
    }

    /**
     *
     * @return the new Injector
     */
    public Injector start() {
        injector = builder.build().createInjector();
        LifecycleManager manager = injector.getInstance(LifecycleManager.class);
        try {
            manager.start();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        return injector;
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
