package com.netflix.governator;

import com.google.inject.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

@Singleton
public final class SimpleProvisionMetrics implements ProvisionMetrics {
    private final ConcurrentMap<Long, Node> threads = new ConcurrentHashMap<>();
    
    private static class Node {
        List<Entry> children = new ArrayList<>();
        Stack<Entry> stack = new Stack<>();
        
        void accept(Visitor visitor) {
            children.forEach(entry -> visitor.visit(entry));
        }
        
        void push(Entry entry) {
            if (stack.isEmpty()) {
                children.add(entry);
            } else {
                stack.peek().add(entry);
            }
            stack.push(entry);
        }
        
        void pop() {
            stack.pop().finish();            
        }
    }
    
    public static class Entry implements Element {
        final Key<?>        key;
        final List<Entry>   children  = new ArrayList<>();
        final long          startTime = System.nanoTime();
        long                endTime;
        
        Entry(Key<?> key) {
            this.key = key;
        }
        
        void add(Entry child) {
            children.add(child);
        }

        void finish() {
            endTime = System.nanoTime();
        }
        
        @Override
        public Key<?> getKey() {
            return key;
        }
        
        @Override
        public void accept(Visitor visit) {
            for (Entry entry : children) {
                visit.visit(entry);
            }
        }

        @Override
        public long getDuration(TimeUnit units) {
            long childDuration = 0;
            for (Entry child : children) {
                childDuration += child.getTotalDuration(units);
            }
            return getTotalDuration(units) - childDuration;
        }
        
        @Override
        public long getTotalDuration(TimeUnit units) {
            return units.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        }
    }
    
    private Node currentNode() {
        return threads.computeIfAbsent(Thread.currentThread().getId(), id -> new Node());
    }
    
    @Override
    public void push(Key<?> key) {
        currentNode().push(new Entry(key));
    }

    @Override
    public void pop() {
        currentNode().pop();
    }
    
    @Override
    public void accept(Visitor visitor) {
        threads.forEach((id, data) -> data.accept(visitor));
    }
}
