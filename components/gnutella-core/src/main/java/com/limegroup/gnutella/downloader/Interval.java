package com.limegroup.gnutella.downloader;

/** The interval from low to high, inclusive on both ends. */
public class Interval {
    /** INVARIANT: low<=high */
    public int low;
    public int high;

    /** @requires low<=high */
    Interval(int low, int high) {
        this.low=low;
        this.high=high;
    }
    Interval(int singleton) {
        this.low=singleton;
        this.high=singleton;
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
