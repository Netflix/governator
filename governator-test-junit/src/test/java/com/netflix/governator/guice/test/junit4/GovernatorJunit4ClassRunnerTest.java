package com.netflix.governator.guice.test.junit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
    @Inject @WrapWithSpy Spied spied;
    
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
    
    @Test
    public void testMocksCleanUpAfterTestsPartOne() {
        assertNotNull(toBeMocked);
        toBeMocked.invoke();
        Mockito.verify(toBeMocked, Mockito.times(1)).invoke();   
    }
    
    @Test
    public void testMocksCleanUpAfterTestsPartTwo() {
        assertNotNull(toBeMocked);
        Mockito.verifyZeroInteractions(toBeMocked);
    }
    
    @Test
    public void testWrapWithSpy() {
        assertNotNull(spied);
        assertTrue(mockUtil.isSpy(spied));
    }

}

class TestInjectorRuleTestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("testString")).toInstance("Test");
        bind(ToBeMocked.class);
        bind(InvokesMock.class);
        bind(NamedMock.class).annotatedWith(Names.named("namedMock"));
        bind(Spied.class).toInstance(new Spied());
    }

}

class ToBeMocked {
    public void invoke() {}
}

class NewBindingToBeMocked {}

class NamedMock {}

class InvokesMock {
    @Inject ToBeMocked mocked;
}

class Spied {
    public void invoke() {};
}
