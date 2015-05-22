package com.netflix.governator.guice.actions;

import com.google.inject.Injector;
import com.netflix.governator.guice.Grapher;
import com.netflix.governator.guice.PostInjectorAction;

public class GrapherAction implements PostInjectorAction {
    private String text;
    
    @Override
    public void call(Injector injector) {
        Grapher grapher = injector.getInstance(Grapher.class);
        try {
            text = grapher.graph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public String getText() {
        return text;
    }
}
