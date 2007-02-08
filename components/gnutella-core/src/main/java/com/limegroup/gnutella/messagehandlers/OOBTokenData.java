/**
 * 
 */
package com.limegroup.gnutella.messagehandlers;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import org.limewire.io.NetworkUtils;
import org.limewire.security.SecurityToken;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.ReplyHandler;

/**
 * Data necessary to build a QueryKey for the OOB protocol (<tt>OOBQueryKey</tt>)
 *
 */
public class OOBTokenData implements SecurityToken.TokenData {
    
    private final int numRequests;
    private final byte [] data;
    
    public OOBTokenData(ReplyHandler replyHandler, byte [] guid, int numRequests) {
        this(replyHandler.getInetAddress(), replyHandler.getPort(), guid, numRequests);        
    }
    
    public OOBTokenData(InetAddress address, int port, byte[] guid, int numRequests) {
        if (numRequests < 0 || numRequests > 255) {
            throw new IllegalArgumentException("requestNum to small or too large " + numRequests);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(35);
        DataOutputStream data = new DataOutputStream(baos);
        try {
            data.writeShort(port);
            if (NetworkUtils.isIPv6Compatible(address)) {
                data.write(NetworkUtils.getIPv6AddressBytes(address));
            }
            else {
                // unkown inet address, write its full address bytes
                data.write(address.getAddress());
            }
            data.write(numRequests);
            data.write(guid);
        }
        catch (IOException ie) {
            ErrorService.error(ie);
        }
        this.data = baos.toByteArray();
        this.numRequests = numRequests;
        
    }
    public byte [] getData() {
        return data;
    }
    
    public int getNumRequests() {
        return numRequests;
    }
}