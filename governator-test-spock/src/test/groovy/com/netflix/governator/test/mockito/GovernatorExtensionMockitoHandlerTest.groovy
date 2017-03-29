package com.netflix.governator.test.mockito;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject
import javax.inject.Named

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil

import spock.lang.Specification
import spock.lang.Stepwise;

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Names
import com.netflix.governator.guice.test.ModulesForTesting
import com.netflix.governator.guice.test.ReplaceWithMock
import com.netflix.governator.guice.test.WrapWithSpy
import com.netflix.governator.guice.test.mocks.mockito.MockitoMockHandler;

@ModulesForTesting(value=TestInjectorRuleTestModule, mockHandler=MockitoMockHandler)
@Stepwise
public class GovernatorExtensionMockitoHandlerTest extends Specification {
    
    MockUtil mockUtil = new MockUtil();

    @Inject
    @Named("testString")
    String testString

    @Inject @ReplaceWithMock ToBeMocked toBeMocked
    @Inject @ReplaceWithMock NewBindingToBeMocked newBindingToBeMocked
    @Named("namedMock")
    @Inject @ReplaceWithMock(name="namedMock") NamedMock namedMock
    @Inject InvokesMock invokesMock
    @Inject @WrapWithSpy Spied spied
    @Inject @WrapWithSpy(name="namedSpied") Spied namedSpied
    @Inject @Named("notSpied") Spied notSpied
    @Inject InvokesSpy invokesSpy
    

    
    def "basicInjectionTest"() {
        expect: 
            testString == "Test"
    }
    def "testAddingMockAsNewBinding"() {
        expect:
            newBindingToBeMocked != null
            mockUtil.isMock(newBindingToBeMocked) == true
    }

    def "testMockingOverridesBinding"() {
        expect:
            invokesMock != null
            toBeMocked != null
            invokesMock.mocked != null
            mockUtil.isMock(toBeMocked) == true
    }
    
    def "testMockingNamedBindings"() {
        expect:
            namedMock != null
            mockUtil.isMock(namedMock) == true
    }
    
    def "za_testMocksCleanUpAfterTestsPartOne"() {
        when:
            toBeMocked.invoke()
        then:
        
            toBeMocked != null
            Mockito.verify(toBeMocked, Mockito.times(1)).invoke()
    }
    
    def "zz_testMocksCleanUpAfterTestsPartTwo"() {
        expect:
            toBeMocked != null
            Mockito.verifyZeroInteractions(toBeMocked)
    }
    
    def "testWrapWithSpy"() {
        when:
            invokesSpy.invoke()
        then:
            spied != null
            invokesSpy != null
            mockUtil.isSpy(spied) == true
            mockUtil.isSpy(namedSpied) == true
            mockUtil.isSpy(notSpied) == false
            mockUtil.isSpy(invokesSpy.spied) == true
            spied == invokesSpy.spied
            Mockito.verify(spied, Mockito.times(1)).invoke()
    }
}

class TestInjectorRuleTestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("testString")).toInstance("Test");
        bind(ToBeMocked.class);
        bind(InvokesMock.class);
        bind(NamedMock.class).annotatedWith(Names.named("namedMock"));
        bind(InvokesSpy.class);
    }
    
    @Provides
    @Singleton
    public Spied spied() {
        return new Spied();
    }
    
    @Provides
    @Singleton
    @Named("namedSpied")
    public Spied namedSpied() {
        return new Spied();
    }
    
    @Provides
    @Singleton
    @Named("notSpied")
    public Spied notSpied() {
        return new Spied();
    }

}

class ToBeMocked {
    public void invoke() {}
}

class NewBindingToBeMocked {}

class NamedMock {}

@Singleton
class InvokesMock {
    @Inject ToBeMocked mocked;
}

@Singleton
class Spied {
    public void invoke() {};
}

@Singleton
class InvokesSpy {
    @Inject Spied spied;
    
    public void invoke() {
        spied.invoke();
    }
}
