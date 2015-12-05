package com.netflix.governator.internal;

import com.netflix.governator.GovernatorFeature;

/**
 * Container of Governator features.
 */
public interface GovernatorFeatureSet {
    /**
     * @return Get the value of the feature or the default if none is set
     */
    <T> T get(GovernatorFeature<T> feature);
}
