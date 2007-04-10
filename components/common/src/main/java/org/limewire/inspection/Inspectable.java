package org.limewire.inspection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * interface to be implemented by objects that wish to
 * be inspected by InspectionUtils 
 */
public interface Inspectable {
    /**
     * Annotation for fields that wish to be
     * inspectable by InspectionUtils through the
     * String.valueOf() method 
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InspectablePrimitive {}
    
    /**
     * Annotation for objects that have a size() method and
     * wish to be inspected by InspectionUtils with the value
     * of that method.
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InspectableForSize {}

    /**
     * @return a descriptive string for this object.
     */
    public String inspect();
}
