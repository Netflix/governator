package com.netflix.governator.auto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import com.google.inject.Module;
import com.netflix.governator.auto.annotations.ConditionalOnProfile;

/**
 * Load Module.class modules from the ServerLoader but filter out any modules
 * that have no profile condition.
 * 
 * @author elandau
 *
 */
public class ServiceLoaderModuleProvider implements ModuleListProvider {

    @Override
    public List<Module> get() {
        List<Module> modules = new ArrayList<>();
        Iterator<Module> iter = ServiceLoader.load(Module.class).iterator();
        while (iter.hasNext()) {
            Module module = iter.next();
            if (null != module.getClass().getAnnotation(ConditionalOnProfile.class)) {
                modules.add(module);
            }
        }
        
        return modules;
    }

}
