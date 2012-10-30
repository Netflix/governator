package validation;

import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.Configuration;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@AutoBindSingleton
public class ExampleObject
{
    @Min(5)
    @Max(10)
    @Configuration("value")
    private int         value = 0;

    @NotNull
    @Configuration("str")
    private String      str;

    public int getValue()
    {
        return value;
    }

    public void setValue(int value)
    {
        this.value = value;
    }

    public String getStr()
    {
        return str;
    }

    public void setStr(String str)
    {
        this.str = str;
    }
}
