package warmup;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.WarmUp;

@AutoBindSingleton
public class ExampleObjectA
{
    private final ExampleObjectB b;
    private final ExampleObjectC c;

    @Inject
    public ExampleObjectA(ExampleObjectB b, ExampleObjectC c)
    {
        this.b = b;
        this.c = c;
        System.out.println("b.isWarm() " + b.isWarm());
        System.out.println("c.isWarm() " + c.isWarm());
    }

    @WarmUp
    public void     warmUp() throws InterruptedException
    {
        System.out.println("b.isWarm() " + b.isWarm());
        System.out.println("c.isWarm() " + c.isWarm());

        System.out.println("ExampleObjectA warm up start");
        Thread.sleep(1000);
        System.out.println("ExampleObjectA warm up end");
    }
}
