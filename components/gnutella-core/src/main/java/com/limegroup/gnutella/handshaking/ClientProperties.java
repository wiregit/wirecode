package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;

/**
 * Properties for connection handshake, if the node is a client
 */
public class ClientProperties extends LazyProperties{

    public ClientProperties(String remoteIP) {
        this(remoteIP, false, false, null);
    }

    public ClientProperties(String remoteIP, boolean tcpConnectBack,
                            boolean udpConnectBack, byte[] GUID) {
        super(remoteIP, tcpConnectBack, udpConnectBack, GUID);
        //set supernode property
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "False");
        addCommonProperties(this);
    }
}
