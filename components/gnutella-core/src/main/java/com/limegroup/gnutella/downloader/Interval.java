package com.limegroup.gnutella.downloader;

import java.io.Serializable;



import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;;

/** The open interval [low, high] inclusive on the both ends. */
public class Interval implements Serializable{
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
