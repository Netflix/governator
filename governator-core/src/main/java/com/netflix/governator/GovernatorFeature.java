package com.netflix.governator;

/**
 * Base interface for all governator features to be implemented by an 
 * enum, such as {@link GovernatorFeatures}.  Each feature has an implicit
 * default value if not specified.  Features are set on GovernatorConfiguration.
 * 
 * @author elandau
 */
public interface GovernatorFeature {
    public boolean isEnabledByDefault();
}
