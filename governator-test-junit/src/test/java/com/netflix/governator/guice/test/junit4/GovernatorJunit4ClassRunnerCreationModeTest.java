package com.netflix.governator.guice.test.junit4;

import static org.junit.Assert.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import com.netflix.governator.guice.test.ModulesForTesting;
import com.netflix.governator.guice.test.InjectorCreationMode;

@RunWith(Enclosed.class)
public class GovernatorJunit4ClassRunnerCreationModeTest {

    @RunWith(GovernatorJunit4ClassRunner.class)
    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    @ModulesForTesting()
    public static class ClassLevelTest {
        
        @Inject
        StatefulTestBinding stateful;
        
        @Test
        public void test1() {
            assertEquals(null, stateful.getState());
            stateful.setState("foo");
        }
        
        @Test
        public void test2() {
            assertEquals("foo", stateful.getState());
            stateful.setState("foo");
        }

    }
    
    @RunWith(GovernatorJunit4ClassRunner.class)
    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    @ModulesForTesting(injectorCreation=InjectorCreationMode.BEFORE_EACH_TEST_METHOD)
    public static class MethodLevelTest {
        
        @Inject
        StatefulTestBinding stateful;
        
        @Test
        public void test1() {
            assertNull(stateful.getState());
            stateful.setState("foo");
        }
        
        @Test
        public void test2() {
            assertNull(stateful.getState());
        }

    }

}

@Singleton
class StatefulTestBinding {
    private String state;

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
