package com.netflix.governator.guice;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.netflix.governator.guice.actions.GrapherAction;

public class TestPostInjectAction {
    @Test
    public void testPostInjectReport() {
        GrapherAction action = new GrapherAction();
        LifecycleInjector.builder()
            .withPostInjectorAction(action)
        .build()
        .createInjector();
        
        Assert.assertNotNull(action.getText());
    }
}
