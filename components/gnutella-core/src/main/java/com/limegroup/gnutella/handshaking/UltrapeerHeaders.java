package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;

/**
 * Headers that should be sent only by Ultrapeers.
 */
public class UltrapeerHeaders extends DefaultHeaders {
    
    /**
     * Creates a new <tt>UltrapeerHeaders</tt> instance with the specified
     * remote IP.
     *
     * @param remoteIP the IP address of this node as seen by other nodes
     *  on Gnutella -- useful in discovering the real address at the NAT
     *  or firewall
     */
    public UltrapeerHeaders(String remoteIP) {
        super(remoteIP);
        //set Ultrapeer property
        put(HeaderNames.X_ULTRAPEER, "True");
    }
    
}

