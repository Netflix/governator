/*
 * Copyright 2016 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.guice.servlet;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.governator.LifecycleShutdownSignal;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * @author Nikos Michalakis <nikos@netflix.com>
 */
public class GovernatorServletContextListenerTest {

    private GovernatorServletContextListener listener;
    private LifecycleShutdownSignal mockLifecycleSignal;

    @Before
    public void setUp() {
        mockLifecycleSignal = Mockito.mock(LifecycleShutdownSignal.class);
        listener = new GovernatorServletContextListener() {
            @Override
            protected Injector createInjector() throws Exception {
                return Guice.createInjector(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(LifecycleShutdownSignal.class).toInstance(mockLifecycleSignal);
                    }
                });
            }
        };
    }

    @Test(expected = IllegalStateException.class)
    public void testGetInjectorIsCreatedOnlyOnce() {
        Injector injector = listener.getInjector();
        Assert.assertNotNull(injector);
        listener.getInjector();
    }

    @Test
    public void testContextDestroyedAfterInjectorIsCreated() {
        // The app starts and gets an injector.
        listener.getInjector();
        // Now we make sure it's signalled when we shutdown.
        ServletContextEvent servletContextEvent = Mockito.mock(ServletContextEvent.class);
        ServletContext mockServletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContextEvent.getServletContext()).thenReturn(mockServletContext);
        listener.contextDestroyed(servletContextEvent);
        Mockito.verify(mockLifecycleSignal).signal();
    }
}
