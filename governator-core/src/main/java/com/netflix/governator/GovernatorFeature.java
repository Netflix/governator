package com.netflix.governator;

/**
 * Base interface for all governator features to be implemented by an 
 * enum, such as {@link GovernatorFeatures}.  Each feature has an implicit
 * default value if not specified.  Features are set on GovernatorConfiguration.
 * 
 * @author elandau
 * @deprecated Functionality moved https://github.com/Netflix/karyon/tree/3.x
 */
@Deprecated
public interface GovernatorFeature {
    public boolean isEnabledByDefault();
}
