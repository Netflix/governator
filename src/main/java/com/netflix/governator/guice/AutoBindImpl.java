package com.netflix.governator.guice;

import com.netflix.governator.annotations.AutoBind;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
class AutoBindImpl implements AutoBind
{
    private final String value;

    AutoBindImpl(String value)
    {
        this.value = value;
    }

    public String value()
    {
        return this.value;
    }

    public int hashCode()
    {
        // This is specified in java.lang.Annotation.
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    public boolean equals(Object o)
    {
        if ( !(o instanceof AutoBind) )
        {
            return false;
        }

        AutoBind other = (AutoBind)o;
        return value.equals(other.value());
    }

    public String toString()
    {
        return "@" + AutoBind.class.getName() + "(value=" + value + ")";
    }

    public Class<? extends Annotation> annotationType()
    {
        return AutoBind.class;
    }

    @SuppressWarnings("UnusedDeclaration")
    private static final long serialVersionUID = 0;
}
