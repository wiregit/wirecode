package com.limegroup.gnutella.downloader;

import java.io.Serializable;

import com.limegroup.gnutella.Assert;

/** The half-open interval [low, high) inclusive on the low end. */
public class Interval implements Serializable {
    /** Ensure backwards compatibility. */
    static final long serialVersionUID = -2562093104400487554L;

    /** INVARIANT: low<=high */
    public int low;
    public int high;

    /** @requires low<=high */
    public Interval(int low, int high) {
        this.low=low;
        this.high=high;
    }
    public Interval(int singleton) {
        this.low=singleton;
        this.high=singleton;
    }

    /** 
     * True if this and other are adjacent, i.e. the high end of one equals the
     * low end of the other.  
     */
    public boolean adjacent(Interval other) {
        return high==other.low || low==other.high;
    }

    /**
     * True if this and other overlap.  
     */
    public boolean overlaps(Interval other) {
        return (this.low<other.high && other.low<this.high) 
            || (other.low<this.high && this.low<other.high);
    }

    public String toString() {
        if (low==high)
            return String.valueOf(low);
        else
            return String.valueOf(low)+"-"+String.valueOf(high);
    }

    public boolean equals(Object o) {
        if (! (o instanceof Interval))
            return false;
        Interval other=(Interval)o;
        return low==other.low && high==other.high;
    }

}
