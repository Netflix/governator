package com.netflix.governator;

import java.util.concurrent.TimeUnit;

import com.google.inject.ImplementedBy;
import com.google.inject.Key;

/**
 * Interface invoked by LifecycleModule's ProvisionListener to 
 * gather metrics on objects as they are provisioned.  Through the
 * provision listener it's possible to generate a dependency tree
 * for the first initialization of all objects.  Note that no call
 * will be made for singletons that are being injected but have 
 * already been instantiated.
 * 
 * @author elandau
 */
@ImplementedBy(NullProvisionMetrics.class)
public interface ProvisionMetrics {
    
    /**
     * Node used to track metrics for an object that has been provisioned
     * @author elandau
     */
    public static interface Element {
        public Key<?> getKey();
        
        public long getDuration(TimeUnit units);
        
        public long getTotalDuration(TimeUnit units);
        
        public void accept(Visitor visitor);
    }
    
    /**
     * Visitor API for traversing nodes
     * @author elandau
     *
     */
    public static interface Visitor {
        void visit(Element element);
    }
    
    /**
     * Notification that an object of type 'key' is about to be created.
     * Note that there will likely be several nested calls to push() as
     * dependencies are injected.
     * @param key
     */
    public void push(Key<?> key);
    
    /**
     * Pop and finalize initialization of the latest object to be provisioned.
     * A matching pop will be called for each push(). 
     */
    public void pop();
    
    /**
     * Traverse the elements using the visitor pattern.
     * @param visitor
     */
    public void accept(Visitor visitor);
}
