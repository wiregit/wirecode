package org.limewire.service;

/**
 * A switch is an interface that objects can use
 * when they want to allow a boolean setting to
 * be retrieved and/or changed.
 */
public interface Switch {
    public boolean getValue();
    public void setValue(boolean b);

}
