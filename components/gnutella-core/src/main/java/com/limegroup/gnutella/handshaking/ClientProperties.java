package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;

/**
 * Properties for connection handshake, if the node is a client
 */
public class ClientProperties extends LazyProperties{

    public ClientProperties(MessageRouter router){
        super(router);
        //set supernode property
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "False");
        addCommonProperties(this);
    }
}