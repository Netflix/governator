package com.netflix.governator.lifecycle.resources;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Singleton;

/**
 * @author Alexey Krylov (lexx)
 * @since 19.02.13
 */
@Singleton
public class Jsr250EnabledService
{
    private boolean postConstuctInvoked;
    private Jsr250Resource resource;
    private boolean preDestroyInvoked;

    @PostConstruct
    protected void postConstuct()
    {
        postConstuctInvoked = true;
    }

    @Resource
    public void setResource(Jsr250Resource resource)
    {
        this.resource = resource;
    }

    public boolean isPostConstuctInvoked()
    {
        return postConstuctInvoked;
    }

    public boolean isResourceSet()
    {
        return resource != null;
    }

    @PreDestroy
    protected void preDestroy()
    {
        preDestroyInvoked = true;
    }

    public boolean isPreDestroyInvoked()
    {
        return preDestroyInvoked;
    }

    public Jsr250Resource getResource()
    {
        return resource;
    }
}
