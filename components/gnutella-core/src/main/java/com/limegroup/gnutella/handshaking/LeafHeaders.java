package com.limegroup.gnutella.handshaking;

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
    public LeafHeaders(String remoteIP){
        super(remoteIP);
        //set Ultrapeer property
        put(HeaderNames.X_ULTRAPEER, "False");
    }
}
