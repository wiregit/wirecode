package org.limewire.collection;

import java.io.Serializable;

import org.limewire.util.ByteOrder;


/** 
 * Represents a closed interval [low, high] inclusive on both ends. 
 */
public class Interval implements Serializable{
    /** Ensure backwards compatibility. */
    static final long serialVersionUID = -2562093104400487554L;

    /** INVARIANT: low<=high */
    public final int low;
    public final int high;

    /** Requires low<=high.
     *  Requires low and high can be represented as ints.
     */
    public Interval(long low, long high) {
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
    *  Requires singleton can be represented as an int.
    */
    public Interval(long singleton) {
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
    	toBytes(res,0);
    	return res;
    }
    
    public void toBytes(byte [] dest, int offset) {
        ByteOrder.int2beb(low,dest,offset);
        ByteOrder.int2beb(high,dest,offset+4);
    }
   

}
