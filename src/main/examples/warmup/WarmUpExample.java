package warmup;

import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

public class WarmUpExample
{
    public static void main(String[] args) throws Exception
    {
        // Always get the Guice injector from Governator
        Injector injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(ExampleObjectA.class);

        LifecycleManager manager = injector.getInstance(LifecycleManager.class);

        // Always start the Lifecycle Manager
        manager.start();

        // by calling start() warm up begins. The console will show
        // something like this (the order may be slightly different):
        /*
            b.isWarm() false
            c.isWarm() false
            ExampleObjectB warm up start
            ExampleObjectC warm up start
            ExampleObjectB warm up end
            ExampleObjectC warm up end
            b.isWarm() true
            c.isWarm() true
            ExampleObjectA warm up start
            ExampleObjectA warm up end
         */

        // your app would execute here

        // Always close the Lifecycle Manager at app end
        manager.close();
    }
}
