package configuration;

import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.PreConfiguration;

public class ExampleObject
{
    @Configuration("${prefix}.a-string")
    private String      aString = "default value";

    @Configuration("${prefix}.an-int")
    private int         anInt = 0;

    @Configuration("${prefix}.a-double")
    private double      aDouble = 0;

    @PreConfiguration
    public void     preConfig()
    {
        System.out.println("preConfig");
    }

    public String getAString()
    {
        return aString;
    }

    public int getAnInt()
    {
        return anInt;
    }

    public double getADouble()
    {
        return aDouble;
    }

    public void setAString(String aString)
    {
        this.aString = aString;
    }

    public void setAnInt(int anInt)
    {
        this.anInt = anInt;
    }

    public void setADouble(double aDouble)
    {
        this.aDouble = aDouble;
    }
}
