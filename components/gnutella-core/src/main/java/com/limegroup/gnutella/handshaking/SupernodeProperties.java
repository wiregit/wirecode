package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;

/**
 * Properties for connection handshake, if the node is a supernode
 */
public class SupernodeProperties extends LazyProperties
{
    
    public SupernodeProperties(String remoteIP)
    {
        super(remoteIP);
        //set supernode property
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "True");
        put(ConnectionHandshakeHeaders.X_DEGREE, 
			Integer.toString(ConnectionManager.ULTRAPEER_CONNECTIONS));
		put(ConnectionHandshakeHeaders.X_ULTRAPEER_QUERY_ROUTING, 
			ConnectionHandshakeHeaders.QUERY_ROUTING_VERSION);
        addCommonProperties(this);
    }
    
}

