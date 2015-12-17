package com.netflix.governator;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.spi.InjectorCreator;

public class SimpleInjectorCreator implements InjectorCreator<Injector> {
    @Override
    public Injector createInjector(Stage stage, Module module) {
        return Guice.createInjector(stage, module);
    }

}
