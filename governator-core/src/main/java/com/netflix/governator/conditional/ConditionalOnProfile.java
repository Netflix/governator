package com.netflix.governator.conditional;

import java.util.Set;

import com.google.inject.Inject;
import com.netflix.governator.annotations.binding.Profiles;

final public class ConditionalOnProfile extends AbstractConditional {
    private final String profile;

    @Inject(optional=true)
    @Profiles
    Set<String> profiles;
    
    public ConditionalOnProfile(String profile) {
        this.profile = profile;
    }

    @Override
    public boolean evaluate() {
        if (profiles == null) {
            return false;
        }
        return profiles.contains(profile);
    }
    
    @Override
    public String toString() {
        return "ConditionalOnProfile[" + profile + "]";
    }
}
