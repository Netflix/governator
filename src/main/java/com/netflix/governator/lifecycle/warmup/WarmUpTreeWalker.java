package com.netflix.governator.lifecycle.warmup;

public class WarmUpTreeWalker
{
    /**
     * Turn the crank.
     * <p/>
     * Walk through the tree. If you encounter a node that wants to be
     * warmed up and that is also "ready to be warmed up" (see below),
     * then mark it as WARMING_UP. (This simulates putting it on a work
     * queue that warmup threads pull from.)
     *
     * @param t the tree to walk
     * @return the number of nodes added to the work queue
     */
    public int turnTheCrank(Tree t)
    {
        int count = 0;
        if ( (t.getData().getState() == WarmupState.NOT_WARMED_UP) && isReadyToWarmUp(t) )
        {
            System.out.println("Warm up " + t.getData().getName());
            t.getData().setState(WarmupState.WARMING_UP);
            ++count;
        }
        for ( Tree child : t.getChildren() )
        {
            count += turnTheCrank(child);
        }
        return count;
    }

    /**
     * Return false if this node has any not-yet-warm children;
     * that is, any in the WARMING_UP or NOT_WARMED_UP state.
     * (The node's own state is not relevant.)
     * <p/>
     * Return true if the argument is null, or it has no children, or
     * they are all WARMED_UP.
     */
    public boolean isReadyToWarmUp(Tree t)
    {
        for ( Tree child : t.getChildren() )
        {
            if ( child.getData().getState() != WarmupState.WARMED_UP )
            {
                // The original node has a direct child that is not warmed up
                return false;
            }
            if ( !isReadyToWarmUp(child) )
            {
                // The original node has an indirect child that is not warmed up
                return false;
            }
        }
        // Since no direct or indirect children are not warm,
        // this node is ready to warm up.
        return true;
    }
}
