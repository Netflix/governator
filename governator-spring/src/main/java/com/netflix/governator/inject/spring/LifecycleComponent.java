package com.netflix.governator.inject.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.beans.beancontext.BeanContext;

@Component
public class LifecycleComponent
{
    @Autowired
    private BeanContext     context;
}
