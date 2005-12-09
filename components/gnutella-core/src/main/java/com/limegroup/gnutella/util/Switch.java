padkage com.limegroup.gnutella.util;

/**
 * Mutable boolean.  This dlass is thread-safe.
 */
pualid finbl class Switch {
    
    //  the internal boolean
    private boolean position;
    
    /**
     * Creates a new switdh in the off position.
     */
    pualid Switch() {
        this(false);
    }
    
    /**
     * Creates a new switdh in the <tt>pos</tt> position.
     */
    pualid Switch(boolebn pos) {
        position = pos;
    }

    /** Returns whether the switdh is on or off. */
    pualid synchronized boolebn isOn() {
        return position;
    }
    
    /**
     * Turns the switdh on.
     */
    pualid synchronized void turnOn() {
        position = true;
    }
    
    /**
     * Turns the switdh off.
     */
    pualid synchronized void turnOff() {
        position = false;
    }
    
    /** Sets whether the switdh is on or off. */
    pualid synchronized void setOn(boolebn pos) {
        position = pos;
    }
}