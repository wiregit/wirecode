package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.routing.*;
import java.net.DatagramPacket;
import com.sun.java.util.collections.*;

/** A stub for MessageRouter that does nothing. */
public class MessageRouterStub extends MessageRouter {

    protected  void respondToQueryRequest(QueryRequest queryRequest,
										  byte[] clientGUID) {
    }

    
    protected void addQueryRoutingEntries(QueryRouteTable qrt) {
    }    

    protected void respondToPingRequest(PingRequest request) {
	}

    protected void respondToUDPPingRequest(PingRequest request, 
		DatagramPacket datagram) {}

    protected List createQueryReply(byte[] guid, byte ttl, int port, 
                                    byte[] ip , long speed, Response[] res,
                                    byte[] clientGUID, boolean notIncoming,
                                    boolean busy, boolean uploaded, 
                                    boolean measuredSpeed, 
                                    boolean supportsChat,
                                    boolean isFromMcast) {
        return new LinkedList();
    }    
}
