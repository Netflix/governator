package com.netflix.governator.auto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import com.google.inject.Module;

public class ServiceLoaderModuleProvider implements ModuleProvider {

    @Override
    public List<Module> get() {
        List<Module> modules = new ArrayList<>();
        Iterator<Module> iter = ServiceLoader.load(Module.class).iterator();
        while (iter.hasNext()) {
            modules.add(iter.next());
        }
        
        return modules;
    }

}
