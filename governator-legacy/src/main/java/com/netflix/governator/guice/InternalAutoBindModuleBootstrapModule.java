package com.netflix.governator.guice;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.lifecycle.ClasspathScanner;

/**
 * Custom BootstrapModule that auto-installs guice modules annotated with AutoBindSingleton
 * @author elandau
 *
 */
@Singleton
public class InternalAutoBindModuleBootstrapModule implements BootstrapModule {
    private static final Logger LOG = LoggerFactory.getLogger(InternalAutoBindModule.class);

    private final List<Class<?>> ignoreClasses;
    private final ClasspathScanner classpathScanner;

    @Inject
    InternalAutoBindModuleBootstrapModule(ClasspathScanner classpathScanner, Collection<Class<?>> ignoreClasses) {
        this.classpathScanner = classpathScanner;
        Preconditions.checkNotNull(ignoreClasses, "ignoreClasses cannot be null");

        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
    }

    @Override
    public void configure(BootstrapBinder binder) {
        bindAutoBindSingletons(binder);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Module bindAutoBindSingletons(BootstrapBinder binder) {
        List<Module> modules = Lists.newArrayList();
        for (final Class<?> clazz : classpathScanner.getClasses()) {
            if (ignoreClasses.contains(clazz) || !clazz.isAnnotationPresent(AutoBindSingleton.class)) {
                continue;
            }

            AutoBindSingleton annotation = clazz.getAnnotation(AutoBindSingleton.class);
            if (Module.class.isAssignableFrom(clazz)) {
                Preconditions.checkState(
                        annotation.value() == AutoBindSingleton.class,
                        "@AutoBindSingleton value cannot be set for Modules");
                Preconditions.checkState(
                        annotation.baseClass() == AutoBindSingleton.class,
                        "@AutoBindSingleton value cannot be set for Modules");
                Preconditions.checkState(
                        !annotation.multiple(),
                        "@AutoBindSingleton(multiple=true) value cannot be set for Modules");

                LOG.info("Found @AutoBindSingleton annotated module : {} ", clazz.getName());
                LOG.info("***** @AutoBindSingleton use for module {} is deprecated as of 2015-10-10.  Modules should be added directly to the injector or via install({}.class). See https://github.com/Netflix/governator/wiki/Auto-Binding", clazz.getName(), clazz.getSimpleName());
                binder.include((Class<? extends Module>) clazz);
            } 
        }
        
        return Modules.combine(modules);
    }
}
