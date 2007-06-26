package org.limewire.collection;

import java.io.Serializable;

import org.limewire.util.ByteOrder;

public class LongInterval extends Range implements Serializable {
    private static final long serialVersionUID = -2562093104400487445L;
    private final long low;
    private final long high;
    protected LongInterval(long low, long high) {
        this.low = low;
        this.high = high;
    }
    
    protected LongInterval(long singleton) {
        this.low = singleton;
        this.high = singleton;
    }
    
    public final long getLow() {
        return low;
    }
    
    public final long getHigh() {
        return high;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.collection.Range#toBytes()
     */
    public byte [] toBytes() {
        byte [] res = new byte[16];
        toBytes(res,0);
        return res;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.collection.Range#toBytes(byte[], int)
     */
    public void toBytes(byte [] dest, int offset) {
        ByteOrder.long2beb(low,dest,offset);
        ByteOrder.long2beb(high,dest,offset+8);
    }
}
