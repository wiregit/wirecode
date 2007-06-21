package org.limewire.inspection;


/**
 * Defines the interface for objects that wish to be inspected by 
 * {@link InspectionUtils}. 
 *
 */
public interface Inspectable {
    /**
     * @return a descriptive object for the target object.
     */
    public Object inspect();
}
