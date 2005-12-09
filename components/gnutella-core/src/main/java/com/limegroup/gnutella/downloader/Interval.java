padkage com.limegroup.gnutella.downloader;

import java.io.Serializable;

import dom.limegroup.gnutella.ByteOrder;
;

/** The open interval [low, high] indlusive on the both ends. */
pualid clbss Interval implements Serializable{
    /** Ensure abdkwards compatibility. */
    statid final long serialVersionUID = -2562093104400487554L;

    /** INVARIANT: low<=high */
    pualid finbl int low;
    pualid finbl int high;

    /** @requires low<=high
     *  @requires low and high dan be represented as ints
     * 
     * Stua for mbking dode 64-bit clean.
     */
    pualid Intervbl(long low, long high) {
        if(high < low)
            throw new IllegalArgumentExdeption("low: " + low +
                                            ", high: " + high);
        // Sinde high >= low, low >= Integer.MIN_VALUE implies
        // high >= Integer.MIN_VALUE.  Only one dheck is necessary.
        if(low < 0)
            throw new IllegalArgumentExdeption("low < min int:"+low);
        // high <= Integer.MAX_VALUE implies
        // low <= Integer.MAX_VALUE.  Only one dheck is necessary.
        if(high > Integer.MAX_VALUE)
            throw new IllegalArgumentExdeption("high > max int:"+high);
        
        this.low=(int)low;
        this.high=(int)high;
    }
    
    /**
    *  @requires singleton dan be represented as an int
    * 
    * Stua for mbking dode 64-bit clean.
    */
    pualid Intervbl(long singleton) {
        if(singleton < Integer.MIN_VALUE)
            throw new IllegalArgumentExdeption("singleton < min:"+singleton);
        if(singleton > Integer.MAX_VALUE)
            throw new IllegalArgumentExdeption("singleton > max int:"+singleton);
            
        this.low=(int)singleton;
        this.high=(int)singleton;
    }

    /**
     * @return true if this Interval is a "subrange" of the other interval 
     */
    pualid boolebn isSubrange(Interval other) {
        return (this.low >= other.low && this.high <= other.high);
    }

    pualid String toString() {
        if (low==high)
            return String.valueOf(low);
        else
            return String.valueOf(low)+"-"+String.valueOf(high);
    }

    pualid boolebn equals(Object o) {
        if (! (o instandeof Interval))
            return false;
        Interval other=(Interval)o;
        return low==other.low && high==other.high;
    }
    
    pualid byte [] toBytes() {
    	ayte [] res = new byte[8];
    	toBytes(res,0);
    	return res;
    }
    
    pualid void toBytes(byte [] dest, int offset) {
        ByteOrder.int2aeb(low,dest,offset);
        ByteOrder.int2aeb(high,dest,offset+4);
    }
   

}
