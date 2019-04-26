package com.netflix.governator;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.google.inject.Module;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssistedInjectPreDestroyTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testAssistedInject() throws Exception {
        Module assistedInjectModule = new FactoryModuleBuilder()
                .implement(AnInterestingClient.class, InterestingClientImpl.class)
                .build(InterestingClientFactory.class);

        final AnInterestingClient interestingClient;
        try (LifecycleInjector injector = InjectorBuilder.fromModule(assistedInjectModule).createInjector()) {
            interestingClient = injector.getInstance(InterestingClientFactory.class).create("something");

            log.info("Init is all done, I'll pretend to be doing something for a bit");
            Thread.sleep(1_000L);
            Assert.assertFalse(interestingClient.isClosed());

            log.info("Kicking the GC to see if it triggers unintended cleanups");
            System.gc();
            Thread.sleep(1_000L);
            Assert.assertFalse(interestingClient.isClosed());

            log.info("Just woke up again, about to close shop");
            interestingClient.setOkToClose();
        }
        // injector is closed by try block exit above, which triggers PreDestroy /
        // AutoCloseable methods
        Assert.assertTrue(interestingClient.isClosed());
        log.info("Done closing shop, bye");
    }
}

interface AnInterestingClient {
    void setOkToClose();

    boolean isClosed();
}

class InterestingClientImpl implements AnInterestingClient {
    private static final Logger logger = LoggerFactory.getLogger(InterestingClientImpl.class);
    private final AtomicBoolean isOkToClose = new AtomicBoolean(false);
    private final AtomicBoolean closedBeforeExpected = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Inject
    InterestingClientImpl(@Assisted String aParameter) {
    }

    @Override
    public void setOkToClose() {
        if (closedBeforeExpected.get()) {
            throw new IllegalStateException("Someone called close before we were ready!");
        }
        isOkToClose.set(true);
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @PreDestroy
    public void close() {
        closed.set(true);
        if (!isOkToClose.get()) {
            closedBeforeExpected.set(true);
            logger.info("Someone called close() on me ({}) and I'm mad about it", this);

        } else {
            logger.info("Someone called close() on me ({}) and I'm ok with that", this);
        }
    }
}

interface InterestingClientFactory {
    AnInterestingClient create(@Assisted String aParameter);
}
