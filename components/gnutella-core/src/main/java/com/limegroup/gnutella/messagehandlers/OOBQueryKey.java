package com.limegroup.gnutella.messagehandlers;

import org.limewire.security.AbstractQueryKey;
import org.limewire.security.InvalidSecurityTokenException;

/**
 * A query key to be used in the OOB v3 protocol.
 */
public class OOBQueryKey extends AbstractQueryKey<OOBTokenData> {
    
    /** 
     * Creates a query key with the provided data.
     * The query key consists of the # of results followed 
     * by the MAC checksum of the data object.
     */
    public OOBQueryKey(OOBTokenData data) {
        super(data);
    }

    /**
     * Creates a key from data received from the network.  The first
     * byte is the # of results, the rest are the checksum to verify against.
     * @throws InvalidSecurityTokenException 
     */
    public OOBQueryKey(byte [] network) throws InvalidSecurityTokenException {
        super(network);
    }
    
    protected byte [] getFromMAC(byte []b, OOBTokenData data) {
        byte [] ret = new byte[b.length+1];
        ret[0] = (byte)data.getNumRequests();
        System.arraycopy(b, 0, ret, 1, b.length);
        return ret;
    }
}
