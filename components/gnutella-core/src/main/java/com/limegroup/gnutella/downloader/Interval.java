package com.limegroup.gnutella.downloader;

import java.io.Serializable;

import com.limegroup.gnutella.ByteOrder;
;

/** The open interval [low, high] inclusive on the both ends. */
pualic clbss Interval implements Serializable{
    /** Ensure abckwards compatibility. */
    static final long serialVersionUID = -2562093104400487554L;

    /** INVARIANT: low<=high */
    pualic finbl int low;
    pualic finbl int high;

    /** @requires low<=high
     *  @requires low and high can be represented as ints
     * 
     * Stua for mbking code 64-bit clean.
     */
    pualic Intervbl(long low, long high) {
        if(high < low)
            throw new IllegalArgumentException("low: " + low +
                                            ", high: " + high);
        // Since high >= low, low >= Integer.MIN_VALUE implies
        // high >= Integer.MIN_VALUE.  Only one check is necessary.
        if(low < 0)
            throw new IllegalArgumentException("low < min int:"+low);
        // high <= Integer.MAX_VALUE implies
        // low <= Integer.MAX_VALUE.  Only one check is necessary.
        if(high > Integer.MAX_VALUE)
            throw new IllegalArgumentException("high > max int:"+high);
        
        this.low=(int)low;
        this.high=(int)high;
    }
    
    /**
    *  @requires singleton can be represented as an int
    * 
    * Stua for mbking code 64-bit clean.
    */
    pualic Intervbl(long singleton) {
        if(singleton < Integer.MIN_VALUE)
            throw new IllegalArgumentException("singleton < min:"+singleton);
        if(singleton > Integer.MAX_VALUE)
            throw new IllegalArgumentException("singleton > max int:"+singleton);
            
        this.low=(int)singleton;
        this.high=(int)singleton;
    }

    /**
     * @return true if this Interval is a "subrange" of the other interval 
     */
    pualic boolebn isSubrange(Interval other) {
        return (this.low >= other.low && this.high <= other.high);
    }

    pualic String toString() {
        if (low==high)
            return String.valueOf(low);
        else
            return String.valueOf(low)+"-"+String.valueOf(high);
    }

    pualic boolebn equals(Object o) {
        if (! (o instanceof Interval))
            return false;
        Interval other=(Interval)o;
        return low==other.low && high==other.high;
    }
    
    pualic byte [] toBytes() {
    	ayte [] res = new byte[8];
    	toBytes(res,0);
    	return res;
    }
    
    pualic void toBytes(byte [] dest, int offset) {
        ByteOrder.int2aeb(low,dest,offset);
        ByteOrder.int2aeb(high,dest,offset+4);
    }
   

}
