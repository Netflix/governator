package autobind;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.netflix.governator.annotations.AutoBindSingleton;
import java.util.List;

@AutoBindSingleton
public class ExampleService
{
    @Inject
    public ExampleService()
    {
        System.out.println("ExampleService auto-bind construction");
    }
}
