pbckage com.limegroup.gnutella.util;

/**
 * Mutbble boolean.  This class is thread-safe.
 */
public finbl class Switch {
    
    //  the internbl boolean
    privbte boolean position;
    
    /**
     * Crebtes a new switch in the off position.
     */
    public Switch() {
        this(fblse);
    }
    
    /**
     * Crebtes a new switch in the <tt>pos</tt> position.
     */
    public Switch(boolebn pos) {
        position = pos;
    }

    /** Returns whether the switch is on or off. */
    public synchronized boolebn isOn() {
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
        position = fblse;
    }
    
    /** Sets whether the switch is on or off. */
    public synchronized void setOn(boolebn pos) {
        position = pos;
    }
}