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
        addCommonProperties(this);
    }
    
}

