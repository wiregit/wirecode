package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * A query key that can write itself to an output stream and
 * query the keysmith for its validity.
 */
public abstract class AbstractQueryKey<T extends SecurityToken.TokenData> implements SecurityToken<T>{

    /** 
     * The Query Key.  
     */
    private byte[] _queryKey;
    
    protected AbstractQueryKey(T data) {
        _queryKey = getFromMAC(QueryKeySmith.getDefaultKeySmith().getKeyBytes(data), data);
    }
    
    protected AbstractQueryKey(byte [] network) {
        if (!isValidBytes(network))
            throw new IllegalArgumentException();
        
        _queryKey = network;
    }
    
    public final boolean isFor(T data) {
        for (byte [] key : QueryKeySmith.getDefaultKeySmith().getAllBytes(data)) {
            if (Arrays.equals(_queryKey,getFromMAC(key,data)))
                return true;
        }
        
        return false;
    }

    public final void write(OutputStream os) throws IOException {
        os.write(_queryKey);
    }

    public final byte[] getBytes() {
        byte[] b = new byte[_queryKey.length];
        System.arraycopy(_queryKey, 0, b, 0, _queryKey.length);
        return b;
    }
    
    /**
     * @param MAC the calculated cryptographic MAC
     * @param data the <tt>TokenData</tt> this QueryKey is created from.
     * @return the payload of this QueryKey as it will appear on the network
     */
    protected abstract byte [] getFromMAC(byte [] MAC, T data);
    
    protected boolean isValidBytes(byte [] network) {
        return network != null && network.length > 0;
    }

}