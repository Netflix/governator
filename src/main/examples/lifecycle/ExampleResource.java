package lifecycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Singleton
public class ExampleResource
{
    @Inject
    public ExampleResource()
    {
        System.out.println("ExampleResource construction");
    }

    @PostConstruct
    public void setup()
    {
        System.out.println("ExampleResource setup");
    }

    @PreDestroy
    public void tearDown()
    {
        System.out.println("ExampleResource tearDown");
    }
}
