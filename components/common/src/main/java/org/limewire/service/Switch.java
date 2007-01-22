package org.limewire.service;

/**
 * A switch is an interface that objects can use
 * when they want to allow a boolean setting to
 * be retrieved and/or changed.
 */
public interface Switch {
    
    /** Returns the current value of the switch. */
    public boolean getValue();
    
    /** Sets the new value of the switch. */
    public void setValue(boolean b);

}
