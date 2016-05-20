package com.netflix.governator.guice.test.junit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.netflix.governator.guice.test.ModulesForTesting;

@RunWith(GovernatorJunit4ClassRunner.class)
@ModulesForTesting({ TestInjectorRuleTestModule.class })
public class GovernatorJunit4ClassRunnerArchaius2IntegrationTest {
    
  
    
}
