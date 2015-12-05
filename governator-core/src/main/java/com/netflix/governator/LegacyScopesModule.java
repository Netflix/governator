package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.netflix.governator.guice.lazy.FineGrainedLazySingleton;
import com.netflix.governator.guice.lazy.FineGrainedLazySingletonScope;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;

public class LegacyScopesModule extends AbstractModule {

    @Override
    protected void configure() {
        bindScope(FineGrainedLazySingleton.class, FineGrainedLazySingletonScope.get());
        bindScope(LazySingleton.class, LazySingletonScope.get());
    }

}
