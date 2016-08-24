package com.netflix.governator.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * simple 'builder' class that constructs an immutable list
 *
 */
public class ListBuilder {
    private final List<String> contents;
    public ListBuilder(String ...strings) {
        this.contents = new ArrayList<>();
        this.contents.addAll(Arrays.asList(strings));
    }
    
    public ListBuilder add(String s) {
        this.contents.add(s);
        return this;
    }
    
    public List<String> build() {
        return Collections.unmodifiableList(new ArrayList<>(contents));
    }
}