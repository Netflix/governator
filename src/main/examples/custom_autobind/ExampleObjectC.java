package custom_autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;

public class ExampleObjectC
{
    private final String value;

    @Inject
    public ExampleObjectC(@ExampleAutoBind(propertyName = "prop-c", defaultValue = "c") String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
}
