package com.netflix.governator.auto;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Module;
import com.netflix.governator.ClassPathModuleListProvider;
import com.netflix.governator.auto.annotations.Bootstrap;
import com.netflix.governator.auto.annotations.Conditional;

/**
 * ClassPath scanner using Guava's ClassPath
 * 
 * @author elandau
 */
public class ClassPathConditionalModuleListProvider extends ClassPathModuleListProvider {

    public ClassPathConditionalModuleListProvider(String... packages) {
        super(Arrays.asList(packages));
    }
    
    public ClassPathConditionalModuleListProvider(List<String> packages) {
        super(packages);
    }
    
    @Override
    protected boolean isAllowed(Class<? extends Module> cls) {
        for (Annotation annot : cls.getAnnotations()) {
            if (null != annot.annotationType().getAnnotation(Bootstrap.class) ||
                null != annot.annotationType().getAnnotation(Conditional.class)) {
                return true;
            }
        }
        return false;
    }
}
