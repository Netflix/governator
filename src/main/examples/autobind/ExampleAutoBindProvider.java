package autobind;

import com.google.inject.Binder;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.guice.AutoBindProvider;
import java.util.concurrent.atomic.AtomicInteger;

public class ExampleAutoBindProvider implements AutoBindProvider<AutoBind>
{
    private final AtomicInteger     counter = new AtomicInteger();

    @Override
    public void configure(Binder binder, AutoBind annotation)
    {
        // this method will get called for each field/argument that is annotated
        // with @AutoBind. NOTE: the fields/methods/constructors must also
        // be annotated with @Inject

        String      value = annotation.value() + " - " + counter.incrementAndGet();
        binder.bind(String.class).annotatedWith(annotation).toInstance(value);
    }
}
