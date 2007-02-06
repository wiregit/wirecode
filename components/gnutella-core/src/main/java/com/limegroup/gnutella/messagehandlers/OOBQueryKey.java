package com.limegroup.gnutella.messagehandlers;

import org.limewire.security.AbstractQueryKey;
import org.limewire.security.QueryKeySmith;

/**
 * A query key to be used in the OOB v3 protocol.
 *
 */
public class OOBQueryKey extends AbstractQueryKey {

    /** The size of the query key */
    private static final int SIZE = 5;
    
    /** The number of results requested by the requestor */
    private final int numResults;
    
    /** 
     * Creates a query key with the provided data.
     * The query key consists of the # of results followed 
     * by the MAC checksum of the data object.
     */
    public OOBQueryKey(OOBTokenData data) {
        byte [] body = QueryKeySmith.getDefaultKeySmith().getKeyBytes(data);
        numResults = data.getNumRequests();
        _queryKey = new byte[body.length+1];
        _queryKey[0] = (byte)numResults;
        System.arraycopy(body, 0, _queryKey, 1, body.length);
    }

    /**
     * Creates a key from data received from the network.  The first
     * byte is the # of results, the rest are the checksum to verify against.
     */
    public OOBQueryKey(byte [] network) {
        if (network == null)
            throw new NullPointerException("network must not be null");
        if (network.length == 0) {
            throw new IllegalArgumentException("network must have non-zero length");
        }
        _queryKey = network;
        this.numResults = network[0];
    }
    
    /**
     * @return true if the current key is valid for the provided
     * <tt>TokenData</tt> object.
     */
    public boolean isFor(TokenData data) {
        if (! (data instanceof OOBTokenData))
            return false;
        
        // check against the number of requests
        OOBTokenData oobData = (OOBTokenData) data;
        if (oobData.getNumRequests() != numResults)
            return false;
        
        // and then check the MAC
        byte [] toCheck = new byte[_queryKey.length - 1];
        System.arraycopy(_queryKey, 1, toCheck, 0, toCheck.length);
        return QueryKeySmith.getDefaultKeySmith().isFor(toCheck, data);
    }
}
