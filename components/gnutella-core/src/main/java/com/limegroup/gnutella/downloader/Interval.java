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

    /*
    public static void main(String args[]) {
        Interval a=new Interval(0,3);
        Interval b=new Interval(3,5);
        Interval c=new Interval(1,4);
        Interval d=new Interval(6,10);
        Interval e=new Interval(0,10);

        Assert.that(a.overlaps(a));
        Assert.that(! a.adjacent(a));

        Assert.that(! a.overlaps(b));
        Assert.that(! b.overlaps(a));
        Assert.that(a.adjacent(b));
        Assert.that(b.adjacent(a));

        Assert.that(a.overlaps(c));
        Assert.that(c.overlaps(a));
        Assert.that(! a.adjacent(c));
        Assert.that(! c.adjacent(a));
        
        Assert.that(! a.overlaps(d));
        Assert.that(! d.overlaps(a));
        Assert.that(! a.adjacent(d));
        Assert.that(! d.adjacent(a));

        Assert.that(e.overlaps(c));
        Assert.that(c.overlaps(a));
    }
    */
}
