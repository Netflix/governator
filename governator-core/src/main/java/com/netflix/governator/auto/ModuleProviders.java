package com.netflix.governator.auto;

import com.google.inject.Module;

public class ModuleProviders {
    public static ModuleProvider from(final Module module) {
        return new ModuleProvider() {
            @Override
            public Module get() {
                return module;
            }
        };
    }
}
