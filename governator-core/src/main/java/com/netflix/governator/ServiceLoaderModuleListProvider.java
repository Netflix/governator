package com.netflix.governator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import com.google.inject.Module;
import com.netflix.governator.auto.annotations.Conditional;

/**
 * Load Module.class modules from the ServerLoader but filter out any modules
 * that have no profile condition.
 * 
 * @author elandau
 *
 */
public class ServiceLoaderModuleListProvider implements ModuleListProvider {
    
    private final Class<? extends Module> type;
    
    public ServiceLoaderModuleListProvider(Class<? extends Module> type) {
        this.type = type;
    }
    
    public ServiceLoaderModuleListProvider() {
        this(Module.class);
    }

    @Override
    public List<Module> get() {
        List<Module> modules = new ArrayList<>();
        Iterator<? extends Module> iter = ServiceLoader.load(type).iterator();
        while (iter.hasNext()) {
            Module module = iter.next();
            if (hasConditionalAnnotation(module.getClass())) {
                modules.add(module);
            }
        }
        
        return modules;
    }

    private boolean hasConditionalAnnotation(Class<? extends Module> type) {
        for (Annotation annot : type.getAnnotations()) {
            if (annot.annotationType().isAnnotationPresent(Conditional.class)) {
                return true;
            }
        }
        return false;
    }
}
