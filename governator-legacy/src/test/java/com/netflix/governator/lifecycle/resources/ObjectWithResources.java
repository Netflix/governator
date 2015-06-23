package com.netflix.governator.lifecycle.resources;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.awt.*;
import java.math.BigInteger;

@Resource(name = "classResource", type = Double.class)
public class ObjectWithResources
{
    @Resource
    private String myResource;

    @Resource(name = "overrideInt")
    private BigInteger myOverrideResource;

    private Point p;
    private Rectangle r;

    @Inject
    public ObjectWithResources()
    {
    }

    @Resource
    public void setP(Point p)
    {
        this.p = p;
    }

    public Point getP()
    {
        return p;
    }

    public Rectangle getR()
    {
        return r;
    }

    @Resource(name = "overrideRect")
    public void setR(Rectangle r)
    {
        this.r = r;
    }

    public String getMyResource()
    {
        return myResource;
    }

    public BigInteger getMyOverrideResource()
    {
        return myOverrideResource;
    }
}
