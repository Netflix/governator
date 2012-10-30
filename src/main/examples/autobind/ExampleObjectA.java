package autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;

public class ExampleObjectA
{
    private final String value;

    @Inject
    public ExampleObjectA(@AutoBind("letter A")String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
}
