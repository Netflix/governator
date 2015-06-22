package com.netflix.governator.guice;

import com.google.common.base.Preconditions;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.ClasspathScanner;

public class LoadersBootstrapModule implements BootstrapModule {

    private ClasspathScanner scanner;

    public LoadersBootstrapModule(ClasspathScanner scanner) {
        this.scanner = scanner;
    }
    
    @Override
    public void configure(BootstrapBinder binder) {
        for ( Class<?> clazz : scanner.getClasses() )
        {
            if ( clazz.isAnnotationPresent(AutoBindSingleton.class) && ConfigurationProvider.class.isAssignableFrom(clazz) )
            {
                AutoBindSingleton annotation = clazz.getAnnotation(AutoBindSingleton.class);
                Preconditions.checkState(annotation.value() == AutoBindSingleton.class, "@AutoBindSingleton value cannot be set for ConfigurationProviders");
                Preconditions.checkState(annotation.baseClass() == AutoBindSingleton.class, "@AutoBindSingleton value cannot be set for ConfigurationProviders");
                Preconditions.checkState(!annotation.multiple(), "@AutoBindSingleton(multiple=true) value cannot be set for ConfigurationProviders");

                @SuppressWarnings("unchecked")
                Class<? extends ConfigurationProvider>    configurationProviderClass = (Class<? extends ConfigurationProvider>)clazz;
                binder.bindConfigurationProvider().to(configurationProviderClass).asEagerSingleton();
            }
        }        
    }

}
