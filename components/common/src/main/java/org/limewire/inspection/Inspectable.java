package org.limewire.inspection;


/**
 * interface to be implemented by objects that wish to
 * be inspected by InspectionUtils 
 */
public interface Inspectable {
    /**
     * @return a descriptive object for the target object.
     */
    public Object inspect();
}
