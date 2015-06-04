package com.netflix.governator;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import com.google.inject.Key;

@Singleton
public class SimpleProvisionMetrics implements ProvisionMetrics {
    private final ThreadLocal<Data> context = new ThreadLocal<Data>();
    private final CopyOnWriteArraySet<Data> threads = new CopyOnWriteArraySet<>();
    
    public static class Data {
        List<Entry> children = new ArrayList<>();
        Stack<Entry> stack = new Stack<>();
        
        void accept(Visitor visit) {
            for (Entry entry : children) {
                visit.visit(entry);
            }
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
                childDuration += child.getDuration(units);
            }
            return getTotalDuration(units) - childDuration;
        }
        
        @Override
        public long getTotalDuration(TimeUnit units) {
            return units.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        }
    }
    
    @Override
    public void push(Key<?> type) {
        Data data = context.get();
        if (data == null) {
            data = new Data();
            context.set(data);
            threads.add(data);
        }
        
        Entry entry = new Entry(type);
        
        if (data.stack.isEmpty()) {
            data.children.add(entry);
        }
        else {
            data.stack.peek().add(entry);
        }
        data.stack.push(entry);
    }

    @Override
    public void pop() {
        Data data = context.get();
        Entry entry = data.stack.pop();
        entry.finish();
    }
    
    @Override
    public void accept(Visitor visitor) {
        for (Data data : threads) {
            data.accept(visitor);
        };
    }
}
