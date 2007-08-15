package com.limegroup.gnutella.util;

/**
 * Mutable boolean.  This class is thread-safe.
 */
public final class Switch {
    
    //  the internal boolean
    private boolean position;
    
    /**
     * Creates a new switch in the off position.
     */
    public Switch() {
        this(false);
    }
    
    /**
     * Creates a new switch in the <tt>pos</tt> position.
     */
    public Switch(boolean pos) {
        position = pos;
    }

    /** Returns whether the switch is on or off. */
    public synchronized boolean isOn() {
        return position;
    }
    
    /**
     * Turns the switch on.
     */
    public synchronized void turnOn() {
        position = true;
    }
    
    /**
     * Turns the switch off.
     */
    public synchronized void turnOff() {
        position = false;
    }
    
    /** Sets whether the switch is on or off. */
    public synchronized void setOn(boolean pos) {
        position = pos;
    }
}