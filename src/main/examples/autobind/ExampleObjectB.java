package autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;

public class ExampleObjectB
{
    private final String value;

    @Inject
    public ExampleObjectB(@AutoBind("letter B") String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
}
