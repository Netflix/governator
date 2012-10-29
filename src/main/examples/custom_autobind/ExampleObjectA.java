package custom_autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;

public class ExampleObjectA
{
    private final String value;

    @Inject
    public ExampleObjectA(@ExampleAutoBind(propertyName = "prop-a", defaultValue = "a") String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
}
