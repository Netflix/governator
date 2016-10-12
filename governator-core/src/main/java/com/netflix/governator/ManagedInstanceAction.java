package com.netflix.governator;

import java.lang.ref.Reference;
import java.util.concurrent.Callable;

/**
 * Runnable that applies one or more LifecycleActions to a managed instance T or Reference<T>.
 * For Reference<T> the action is invoked on a best-effort basis, if the referent is non-null
 * at time run() is invoked
 */
public final class ManagedInstanceAction implements Callable<Void> {
    private final Object target; // the managed instance
    private final Reference<?> targetReference; // reference to the managed instance
    private final Iterable<LifecycleAction> actions; // set of actions that will
                                                     // be applied to target

    public ManagedInstanceAction(Object target, Iterable<LifecycleAction> actions) {
        this.target = target; // keep hard reference to target
        this.targetReference = null;
        this.actions = actions;
    }

    public ManagedInstanceAction(Reference<?> target, Object context, Iterable<LifecycleAction> actions) {
        this.target = null;
        this.targetReference = target; // keep hard reference to target
        this.actions = actions;
    }

    @Override
    public Void call() throws Exception {
        Object target = (targetReference == null) ? this.target : targetReference.get();
        if (target != null) {
            for (LifecycleAction m : actions) {
                m.call(target);
            }
        }
        return null;
    }
}