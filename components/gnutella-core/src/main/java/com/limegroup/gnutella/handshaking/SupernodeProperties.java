package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;

/**
 * Properties for connection handshake, if the node is a supernode
 */
public class SupernodeProperties extends LazyProperties
{
    
    public SupernodeProperties(MessageRouter router)
    {
        super(router);
        //set supernode property
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "True");
        addCommonProperties(this);
    }
    
}

