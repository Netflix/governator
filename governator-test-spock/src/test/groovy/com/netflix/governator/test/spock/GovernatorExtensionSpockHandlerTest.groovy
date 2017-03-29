package com.netflix.governator.test.spock;


import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertTrue

import javax.inject.Inject
import javax.inject.Named

import org.spockframework.mock.MockUtil

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Names
import com.netflix.governator.guice.test.ModulesForTesting
import com.netflix.governator.guice.test.ReplaceWithMock
import com.netflix.governator.guice.test.WrapWithSpy
import com.netflix.governator.test.mock.spock.SpockMockHandler

@ModulesForTesting(value=TestInjectorRuleTestModule)
@Stepwise
public class GovernatorExtensionSpockHandlerTest extends Specification {
    
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
    
    def setupSpec() {
        System.setProperty("org.spockframework.mock.ignoreByteBuddy", "true")
    }
    
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
            mockUtil.isMock(toBeMocked)
            1 * toBeMocked.invoke()
    }
    
    def "zz_testMocksCleanUpAfterTestsPartTwo"() {
        when:
            def doNothing = null
        then:
            toBeMocked != null
            mockUtil.isMock(toBeMocked)
            0 * toBeMocked.invoke()
    }
    
    def "testWrapWithSpy"() {
        when:
            invokesSpy.invoke()
        then:
            spied != null
            invokesSpy != null
            mockUtil.isMock(spied) == true
            mockUtil.isMock(namedSpied) == true
            mockUtil.isMock(notSpied) == false
            mockUtil.isMock(invokesSpy.spied) == true
            spied == invokesSpy.spied
            1 * spied.invoke()
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
