package lifecycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Singleton
public class ExampleService
{
    @Inject
    public ExampleService(ExampleResource resource)
    {
        System.out.println("ExampleService construction");
    }

    @PostConstruct
    public void setup()
    {
        System.out.println("ExampleService setup");
    }

    @PreDestroy
    public void tearDown()
    {
        System.out.println("ExampleService tearDown");
    }
}
