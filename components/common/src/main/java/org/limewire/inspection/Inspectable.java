package org.limewire.inspection;


/**
 * interface to be implemented by objects that wish to
 * be inspected by InspectionUtils 
 */
public interface Inspectable {
    /**
     * @return a descriptive string for this object.
     */
    public String inspect();
}
