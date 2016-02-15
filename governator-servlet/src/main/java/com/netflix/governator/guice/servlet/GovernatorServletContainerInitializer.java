package com.netflix.governator.guice.servlet;

import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;

@HandlesTypes(WebApplicationInitializer.class)
public class GovernatorServletContainerInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> initializerClasses, ServletContext servletContext) throws ServletException {
        final WebApplicationInitializer initializer = getInitializer(initializerClasses, servletContext);
        if (initializer != null) {
            servletContext.addFilter("guiceFilter", new GuiceFilter()).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
            servletContext.addListener(new GovernatorServletContextListener() {
                @Override
                protected Injector createInjector() throws Exception {
                    return initializer.createInjector();
                }
            });
        }
    }

    private WebApplicationInitializer getInitializer(Set<Class<?>> initializerClasses, ServletContext servletContext)
            throws ServletException {
        List<WebApplicationInitializer> initializers = new LinkedList<WebApplicationInitializer>();
        if (initializerClasses != null) {
            for (Class<?> initializerClass : initializerClasses) {
                if (!initializerClass.isInterface() && !Modifier.isAbstract(initializerClass.getModifiers())
                        && WebApplicationInitializer.class.isAssignableFrom(initializerClass)) {
                    try {
                        initializers.add((WebApplicationInitializer) initializerClass.newInstance());
                    } catch (Throwable ex) {
                        throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
                    }
                }
            }
        }

        if (initializers.isEmpty()) {
            servletContext.log("No WebApplicationInitializer types found on classpath");
            return null;
        }
        if (initializers.size() > 1) {
            servletContext.log(
                    "Multiple WebApplicationInitializer types found on classpath. Expected one but found " + initializers.size());
            return null;
        }
        return initializers.get(0);
    }

}
