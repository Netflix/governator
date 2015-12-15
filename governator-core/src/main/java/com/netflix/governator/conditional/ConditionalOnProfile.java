package com.netflix.governator.conditional;

import java.util.Set;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.netflix.governator.annotations.binding.Profiles;

final public class ConditionalOnProfile extends AbstractConditional {
    private static final TypeLiteral<Set<String>> STRING_SET_TYPE = new TypeLiteral<Set<String>>() {};
    
    private final String profile;

    public ConditionalOnProfile(String profile) {
        this.profile = profile;
    }

    @Override
    public boolean matches(Injector injector) {
        Binding<Set<String>> profiles = injector.getExistingBinding(Key.get(STRING_SET_TYPE, Profiles.class));
        if (profiles == null) {
            return false;
        }
        return profiles.getProvider().get().contains(profile);
    }
    
    @Override
    public String toString() {
        return "ConditionalOnProfile[" + profile + "]";
    }
}
