package com.netflix.governator.lifecycle.processors;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.inject.Singleton;
import javax.naming.NamingException;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleAnnotationProcessor;
import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.LifecycleState;
import com.netflix.governator.lifecycle.ResourceLocator;

@Singleton
public class ResourcesLifecycleAnnotationProcessor implements LifecycleAnnotationProcessor {

    private final Injector injector;

    @Inject(optional = true)
    private Set<ResourceLocator> resourceLocators = ImmutableSet.of();

    @Inject
    public ResourcesLifecycleAnnotationProcessor(Injector injector) {
        this.injector = injector;
    }
    
    @Override
    public void process(Object obj, LifecycleMethods methods) throws Exception {
        for ( Field field : methods.fieldsFor(Resources.class) )
        {
            Resources resources = field.getAnnotation(Resources.class);
            for ( Resource resource : resources.value() )
            {
                setFieldResource(obj, field, resource);
            }
        }

        for ( Field field : methods.fieldsFor(Resource.class) )
        {
            Resource resource = field.getAnnotation(Resource.class);
            setFieldResource(obj, field, resource);
        }

        for ( Method method : methods.methodsFor(Resources.class) )
        {
            Resources resources = method.getAnnotation(Resources.class);
            for ( Resource resource : resources.value() )
            {
                setMethodResource(obj, method, resource);
            }
        }

        for ( Method method : methods.methodsFor(Resource.class) )
        {
            Resource resource = method.getAnnotation(Resource.class);
            setMethodResource(obj, method, resource);
        }

        for ( Resources resources : methods.classAnnotationsFor(Resources.class) )
        {
            for ( Resource resource : resources.value() )
            {
                loadClassResource(resource);
            }
        }

        for ( Resource resource : methods.classAnnotationsFor(Resource.class) )
        {
            loadClassResource(resource);
        }
    }

    private void loadClassResource(Resource resource) throws Exception
    {
        if ( (resource.name().length() == 0) || (resource.type() == Object.class) )
        {
            throw new Exception("Class resources must have both name() and type(): " + resource);
        }
        findResource(resource);
    }

    private void setMethodResource(Object obj, Method method, Resource resource) throws Exception
    {
        if ( (method.getParameterTypes().length != 1) || (method.getReturnType() != Void.TYPE) )
        {
            throw new Exception(String.format("%s.%s() is not a proper JavaBean setter.", obj.getClass().getName(), method.getName()));
        }

        String beanName = method.getName();
        if ( beanName.toLowerCase().startsWith("set") )
        {
            beanName = beanName.substring("set".length());
        }
        beanName = Introspector.decapitalize(beanName);

        String siteName = obj.getClass().getName() + "/" + beanName;
        resource = adjustResource(resource, method.getParameterTypes()[0], siteName);
        Object resourceObj = findResource(resource);
        method.setAccessible(true);
        method.invoke(obj, resourceObj);
    }

    private void setFieldResource(Object obj, Field field, Resource resource) throws Exception
    {
        String siteName = obj.getClass().getName() + "/" + field.getName();
        Object resourceObj = findResource(adjustResource(resource, field.getType(), siteName));
        field.setAccessible(true);
        field.set(obj, resourceObj);
    }

    private Resource adjustResource(final Resource resource, final Class siteType, final String siteName)
    {
        return new Resource()
        {
            @Override
            public String name()
            {
                return (resource.name().length() == 0) ? siteName : resource.name();
            }

            /**
             * Method needed for eventual java7 compatibility
             */
            public String lookup()
            {
                return name();
            }

            @Override
            public Class type()
            {
                return (resource.type() == Object.class) ? siteType : resource.type();
            }

            @Override
            public AuthenticationType authenticationType()
            {
                return resource.authenticationType();
            }

            @Override
            public boolean shareable()
            {
                return resource.shareable();
            }

            @Override
            public String mappedName()
            {
                return resource.mappedName();
            }

            @Override
            public String description()
            {
                return resource.description();
            }

            @Override
            public Class<? extends Annotation> annotationType()
            {
                return resource.annotationType();
            }
        };
    }

    private Object findResource(Resource resource) throws Exception
    {
        if ( resourceLocators.size() > 0 )
        {
            final Iterator<ResourceLocator> iterator = resourceLocators.iterator();
            ResourceLocator locator = iterator.next();
            ResourceLocator nextInChain = new ResourceLocator()
            {
                @Override
                public Object locate(Resource resource, ResourceLocator nextInChain) throws Exception
                {
                    if ( iterator.hasNext() )
                    {
                        return iterator.next().locate(resource, this);
                    }
                    return defaultFindResource(resource);
                }
            };
            return locator.locate(resource, nextInChain);
        }
        return defaultFindResource(resource);
    }

    @SuppressWarnings("unchecked")
    private Object defaultFindResource(Resource resource) throws Exception
    {
        if ( injector == null )
        {
            throw new NamingException("Could not find resource: " + resource);
        }

        return injector.getInstance(resource.type());
    }
    
    @Override
    public Collection<Class<? extends Annotation>> getFieldAnnotations() {
        return Collections.unmodifiableList(Arrays.<Class<? extends Annotation>>asList(Resource.class));
    }

    @Override
    public Collection<Class<? extends Annotation>> getMethodAnnotations() {
        return Collections.unmodifiableList(Arrays.<Class<? extends Annotation>>asList(Resource.class));
    }

    @Override
    public Collection<Class<? extends Annotation>> getClassAnnotations() {
        return Collections.unmodifiableList(Arrays.<Class<? extends Annotation>>asList(Resource.class, Resources.class));
    }

    @Override
    public LifecycleState getState() {
        return LifecycleState.SETTING_RESOURCES;
    }

}