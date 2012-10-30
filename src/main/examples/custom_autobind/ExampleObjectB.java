package custom_autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;

public class ExampleObjectB
{
    private final String value;

    @Inject
    public ExampleObjectB(@ExampleAutoBind(propertyName = "prop-b", defaultValue = "b") String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
}
