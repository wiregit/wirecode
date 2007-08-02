package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.NetworkManager;

/**
 * Properties for connection handshake, if the node is a client
 */
public class LeafHeaders extends DefaultHeaders {

    /**
     * Creates a new <tt>LeafHeaders</tt> instance with the specified
     * remote IP.
     *
     * @param remoteIP the IP address of this node as seen by other nodes
     *  on Gnutella -- useful in discovering the real address at the NAT
     *  or firewall
     */
    LeafHeaders(String remoteIP, NetworkManager networkManager){
        super(remoteIP, networkManager);
        //set Ultrapeer property
        put(HeaderNames.X_ULTRAPEER, "False");
    }
}
