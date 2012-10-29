package lifecycle;

import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

public class LifecycleExample
{
    public static void main(String[] args) throws Exception
    {
        // Always get the Guice injector from Governator
        Injector injector = LifecycleInjector.builder().createInjector();

        // This causes ExampleService and its dependency, ExampleResource, to get instantiated
        injector.getInstance(ExampleService.class);

        LifecycleManager manager = injector.getInstance(LifecycleManager.class);

        // Always start the Lifecycle Manager
        manager.start();

        // your app would execute here

        // Always close the Lifecycle Manager at app end
        manager.close();

        /*
            The console output should show:
                ExampleResource construction
                ExampleResource setup
                ExampleService construction
                ExampleService setup
                ExampleService tearDown
                ExampleResource tearDown
         */
    }
}
