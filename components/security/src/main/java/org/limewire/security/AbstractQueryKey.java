package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A query key that can write itself to an output stream and
 * query the keysmith for its validity.
 */
public abstract class AbstractQueryKey implements SecurityToken{

    /** 
     * The Query Key.  
     */
    protected byte[] _queryKey;
    
    public boolean isFor(TokenData data) {
        return QueryKeySmith.getDefaultKeySmith().isFor(getBytes(), data);
    }

    public void write(OutputStream os) throws IOException {
        os.write(_queryKey);
    }

    public byte[] getBytes() {
        byte[] b = new byte[_queryKey.length];
        System.arraycopy(_queryKey, 0, b, 0, _queryKey.length);
        return b;
    }

}