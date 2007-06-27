package org.limewire.collection;

import java.io.Serializable;

import org.limewire.util.ByteOrder;


/** 
 * Represents a closed interval [low, high] inclusive on both ends. 
<pre>
    System.out.println("Interval is " + new Interval(1,4));
    
    Output:
        Interval is 1-4
</pre>
 */
class Interval extends Range implements Serializable {
    /** Ensure backwards compatibility. */
    static final long serialVersionUID = -2562093104400487554L;

    /** INVARIANT: low<=high */
    private final int low;
    private final int high;

    /** @requires low<=high.
     *  @requires low and high can be represented as ints.
     */
    protected Interval(long low, long high) {
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
    *  @requires singleton can be represented as an int.
    */
    protected Interval(long singleton) {
        if(singleton < Integer.MIN_VALUE)
            throw new IllegalArgumentException("singleton < min:"+singleton);
        if(singleton > Integer.MAX_VALUE)
            throw new IllegalArgumentException("singleton > max int:"+singleton);
            
        this.low=(int)singleton;
        this.high=(int)singleton;
    }

    
    /* (non-Javadoc)
     * @see org.limewire.collection.Range#toBytes()
     */
    public byte [] toBytes() {
    	byte [] res = new byte[8];
    	toBytes(res,0);
    	return res;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.collection.Range#toBytes(byte[], int)
     */
    public void toBytes(byte [] dest, int offset) {
        ByteOrder.int2beb(low,dest,offset);
        ByteOrder.int2beb(high,dest,offset+4);
    }
   

    /* (non-Javadoc)
     * @see org.limewire.collection.Range#getLow()
     */
    public final long getLow() {
        return low;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.collection.Range#getHigh()
     */
    public final long getHigh() {
        return high;
    }
    
    public final boolean isLong() {
        return false;
    }
}
