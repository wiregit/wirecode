package com.limegroup.gnutella.downloader;

import java.io.Serializable;



import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;;

/** The half-open interval [low, high) inclusive on the low end. */
public class Interval implements Serializable, Comparable {
    /** Ensure backwards compatibility. */
    static final long serialVersionUID = -2562093104400487554L;

    /** INVARIANT: low<=high */
    public int low;
    public int high;

    /** @requires low<=high */
    public Interval(int low, int high) {
        if(high < low)
            throw new IllegalArgumentException("low: " + low +
                                            ", high: " + high);
        this.low=low;
        this.high=high;
    }
    
    public Interval(int singleton) {
        this.low=singleton;
        this.high=singleton;
    }
    
    /**
     * Compares this to another interval by the 'low' element of the interval.
     * If the low elements are the same, then the high element is compared.
     */
    public int compareTo(Object o) {
        Interval other = (Interval)o;
        if( this.low != other.low )
            return this.low - other.low;
        else
            return this.high - other.high;
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

    /**
     * @return true if this Interval is a "subrange" of the other interval 
     */
    public boolean isSubrange(Interval other) {
        return (this.low >= other.low && this.high <= other.high);
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
    
    public byte [] toBytes() {
    	byte [] res = new byte[8];
    	ByteOrder.int2beb(low,res,0);
    	ByteOrder.int2beb(high,res,4);
    	return res;
    }

}
