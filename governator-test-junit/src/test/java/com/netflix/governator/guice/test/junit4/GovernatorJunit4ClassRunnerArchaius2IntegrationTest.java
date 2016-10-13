package com.netflix.governator.guice.test.junit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.test.TestCompositeConfig;
import com.netflix.archaius.test.TestPropertyOverride;
import com.netflix.governator.guice.test.ModulesForTesting;

@RunWith(GovernatorJunit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ModulesForTesting({GovernatorJunit4ClassRunnerArchaius2IntegrationTestModule.class, ArchaiusModule.class})
@TestPropertyOverride("foo=bar")
public class GovernatorJunit4ClassRunnerArchaius2IntegrationTest {
    
    @Inject
    Config config;
    
    @Inject
    @Named("foo")
    String earlyInitString;
    
    @Test
    public void testConfigWiring() {
        assertNotNull(config);
        assertTrue(config instanceof TestCompositeConfig);
    }
    
    @Test
    public void testClassLevelConfig() {
        assertEquals("bar", config.getString("foo"));
        assertEquals("bar", earlyInitString);
    }
    
    @Test
    @TestPropertyOverride("foo=baz")
    public void testMethodLevelConfigOverridesClassLevelConfig() {
        assertEquals("baz", config.getString("foo"));
        assertEquals("bar", earlyInitString);
    }
    
  
    @Test
    public void zz_testMethodLevelConfigClearedBetweenTests() {
        assertEquals("bar", config.getString("foo"));
        assertEquals("bar", earlyInitString);
    }
}

class GovernatorJunit4ClassRunnerArchaius2IntegrationTestModule extends AbstractModule {
    @Override
    protected void configure() {
        
    }
    
    @Provides
    @Singleton
    @Named("foo")
    public String earlyInitString(Config config) {
        return config.getString("foo");
    }
}