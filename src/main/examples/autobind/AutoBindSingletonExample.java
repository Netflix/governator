package autobind;

import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

public class AutoBindSingletonExample
{
    public static void main(String[] args) throws Exception
    {
        // Always get the Guice injector from Governator
        Injector injector = LifecycleInjector
            .builder()
            .usingBasePackages("autobind")  // specify a package for CLASSPATH scanning so that Governator finds the AutoBindSingleton
            .createInjector();

        // NOTE: ExampleService will be created at this point - you should see "ExampleService auto-bind construction" in the console

        LifecycleManager manager = injector.getInstance(LifecycleManager.class);

        // Always start the Lifecycle Manager
        manager.start();

        // your app would execute here

        // Always close the Lifecycle Manager at app end
        manager.close();
    }
}
