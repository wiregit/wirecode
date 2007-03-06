package com.limegroup.gnutella.messagehandlers;

import org.limewire.security.AbstractSecurityToken;
import org.limewire.security.InvalidSecurityTokenException;

/**
 * A security token to be used in the OOB v3 protocol.
 */
public class OOBSecurityToken extends AbstractSecurityToken<OOBTokenData> {
    
    /** 
     * Creates a security token with the provided data.
     * The query key consists of the # of results followed 
     * by the MAC checksum of the data object.
     */
    public OOBSecurityToken(OOBTokenData data) {
        super(data);
    }

    /**
     * Creates a key from data received from the network.  The first
     * byte is the # of results, the rest are the checksum to verify against.
     * @throws InvalidSecurityTokenException 
     */
    public OOBSecurityToken(byte[] network) throws InvalidSecurityTokenException {
        super(network);
    }
    
    protected byte [] getFromMAC(byte[] b, OOBTokenData data) {
        byte [] ret = new byte[b.length+1];
        ret[0] = (byte)data.getNumRequests();
        System.arraycopy(b, 0, ret, 1, b.length);
        return ret;
    }
}
