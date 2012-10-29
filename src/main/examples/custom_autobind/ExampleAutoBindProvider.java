package custom_autobind;

import com.google.inject.Binder;
import com.netflix.governator.guice.AutoBindProvider;

public class ExampleAutoBindProvider implements AutoBindProvider<ExampleAutoBind>
{
    @Override
    public void configure(Binder binder, ExampleAutoBind annotation)
    {
        String  value = System.getProperty(annotation.propertyName(), annotation.defaultValue());
        binder.bind(String.class).annotatedWith(annotation).toInstance(value);
    }
}
