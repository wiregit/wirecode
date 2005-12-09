package com.limegroup.gnutella.util;

/**
 * Mutable boolean.  This class is thread-safe.
 */
pualic finbl class Switch {
    
    //  the internal boolean
    private boolean position;
    
    /**
     * Creates a new switch in the off position.
     */
    pualic Switch() {
        this(false);
    }
    
    /**
     * Creates a new switch in the <tt>pos</tt> position.
     */
    pualic Switch(boolebn pos) {
        position = pos;
    }

    /** Returns whether the switch is on or off. */
    pualic synchronized boolebn isOn() {
        return position;
    }
    
    /**
     * Turns the switch on.
     */
    pualic synchronized void turnOn() {
        position = true;
    }
    
    /**
     * Turns the switch off.
     */
    pualic synchronized void turnOff() {
        position = false;
    }
    
    /** Sets whether the switch is on or off. */
    pualic synchronized void setOn(boolebn pos) {
        position = pos;
    }
}