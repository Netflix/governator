package com.netflix.governator.conditional;

import java.util.Set;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.netflix.governator.annotations.binding.Profiles;

final public class ConditionalOnProfile implements Conditional<ConditionalOnProfile> {
    private String profile;

    public ConditionalOnProfile(String profile) {
        this.profile = profile;
    }

    @Override
    public Class<? extends Matcher<ConditionalOnProfile>> getMatcherClass() {
        return ConditionalOnProfileMatcher.class;
    }
    
    @Override
    public String toString() {
        return "ConditionalOnProfile[" + profile + "]";
    }

    @Singleton
    final public static class ConditionalOnProfileMatcher implements Matcher<ConditionalOnProfile> {
        @Inject(optional=true)
        @Profiles
        Set<String> profiles;
        
        @Override
        public boolean match(ConditionalOnProfile condition) {
            if (profiles == null) {
                return false;
            }
            return profiles.contains(condition.profile);
        }
    }
}
