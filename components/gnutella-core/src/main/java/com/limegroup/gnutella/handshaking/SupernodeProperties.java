package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;

/**
 * Properties for connection handshake, if the node is a supernode
 */
public class SupernodeProperties extends LazyProperties
{
    
    public SupernodeProperties(String remoteIP) {
        this(remoteIP, false, false, null);
    }
    
    public SupernodeProperties(String remoteIP, boolean tcpConnectBack,
                               boolean udpConnectBack, byte[] GUID) {
        super(remoteIP, tcpConnectBack, udpConnectBack, GUID);
        //set supernode property
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "True");
        addCommonProperties(this);
    }
    
}

