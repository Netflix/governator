package warmup;

import com.netflix.governator.annotations.WarmUp;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExampleObjectC
{
    private final AtomicBoolean isWarm = new AtomicBoolean(false);

    @WarmUp
    public void     warmUp() throws InterruptedException
    {
        System.out.println("ExampleObjectC warm up start");
        Thread.sleep(1000);
        System.out.println("ExampleObjectC warm up end");

        isWarm.set(true);
    }

    public boolean      isWarm()
    {
        return isWarm.get();
    }
}
