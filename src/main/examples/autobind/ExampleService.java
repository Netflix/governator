package autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBindSingleton;

@AutoBindSingleton
public class ExampleService
{
    @Inject
    public ExampleService()
    {
        System.out.println("ExampleService auto-bind construction");
    }
}
