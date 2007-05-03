package org.limewire.service;

/**
 * Defines methods for getting and setting a boolean value.
 */
public interface Switch {
    
    /** Returns the current value of the switch. */
    public boolean getValue();
    
    /** Sets the new value of the switch. */
    public void setValue(boolean b);

}
