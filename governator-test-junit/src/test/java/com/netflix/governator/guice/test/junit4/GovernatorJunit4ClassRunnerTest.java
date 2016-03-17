package com.netflix.governator.guice.test.junit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.MockUtil;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.netflix.governator.guice.ModulesForTesting;
import com.netflix.governator.guice.test.ReplaceWithMock;

@RunWith(GovernatorJunit4ClassRunner.class)
@ModulesForTesting({ TestInjectorRuleTestModule.class })
public class GovernatorJunit4ClassRunnerTest {
    
    MockUtil mockUtil = new MockUtil();

    @Inject
    @Named("testString")
    String testString;

    @Inject @ReplaceWithMock ToBeMocked toBeMocked;
    @Inject @ReplaceWithMock NewBindingToBeMocked newBindingToBeMocked;
    @Named("namedMock")
    @Inject @ReplaceWithMock(name="namedMock") NamedMock namedMock;
    @Inject InvokesMock invokesMock;
    
    @Test
    public void basicInjectionTest() {
        assertNotNull(testString);
        assertEquals("Test", testString);
    }

    @Test
    public void testAddingMockAsNewBinding() {
        assertNotNull(newBindingToBeMocked);
        assertTrue(mockUtil.isMock(newBindingToBeMocked));
    }

    @Test
    public void testMockingOverridesBinding() {
        assertNotNull(invokesMock);
        assertNotNull(toBeMocked);
        assertNotNull(invokesMock.mocked);
        assertTrue(mockUtil.isMock(toBeMocked));
    }
    
    @Test
    public void testMockingNamedBindings() {
        assertNotNull(namedMock);
        assertTrue(mockUtil.isMock(namedMock));
    }

}

class TestInjectorRuleTestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("testString")).toInstance("Test");
        bind(ToBeMocked.class).toInstance(new ToBeMocked());
        bind(InvokesMock.class).toInstance(new InvokesMock());
        bind(NamedMock.class).annotatedWith(Names.named("namedMock")).toInstance(new NamedMock());
    }

}

class ToBeMocked {}

class NewBindingToBeMocked {}

class NamedMock {}

class InvokesMock {

    @Inject
    ToBeMocked mocked;

}
