package com.netflix.governator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.reflect.ClassPath;
import com.google.inject.Module;
import com.netflix.governator.auto.annotations.Conditional;

/**
 * ClassPath scanner using Guava's ClassPath
 * 
 * @author elandau
 */
public class ClassPathModuleListProvider implements ModuleListProvider {

    private List<String> packages;

    public ClassPathModuleListProvider(String... packages) {
        this.packages = Arrays.asList(packages);
    }
    
    public ClassPathModuleListProvider(List<String> packages) {
        this.packages = packages;
    }
    
    @Override
    public List<Module> get() {
        List<Module> modules = new ArrayList<>();
        ClassPath classpath;
        for (String pkg : packages) {
            try {
                classpath = ClassPath.from(Thread.currentThread().getContextClassLoader());
                for (ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(pkg)) {
                    try {
                        // Include Modules that have at least on Conditional
                        Class<?> cls = Class.forName(classInfo.getName(), false, ClassLoader.getSystemClassLoader());
                        if (!cls.isInterface() && !Modifier.isAbstract( cls.getModifiers() ) && Module.class.isAssignableFrom(cls)) {
                            for (Annotation annot : cls.getAnnotations()) {
                                if (null != annot.annotationType().getAnnotation(Conditional.class)) {
                                    modules.add((Module) cls.newInstance());
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to instantiate module '" + pkg + "'", e);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to scan root package '" + pkg + "'", e);
            }
        }
        return modules;
    }
}
