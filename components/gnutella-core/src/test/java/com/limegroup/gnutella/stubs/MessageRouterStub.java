package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.routing.*;
import java.net.DatagramPacket;
import com.sun.java.util.collections.*;

/** A stub for MessageRouter that does nothing. */
public class MessageRouterStub extends MessageRouter {

    protected boolean respondToQueryRequest(QueryRequest queryRequest,
										  byte[] clientGUID) {
        return false;
    }

    
    protected void addQueryRoutingEntries(QueryRouteTable qrt) {
    }    

    protected void respondToPingRequest(PingRequest request,
                                        ReplyHandler handler) {
	}

    protected void respondToUDPPingRequest(PingRequest request, 
                                           DatagramPacket datagram,
                                           ReplyHandler handler) {}

    protected List createQueryReply(byte[] guid, byte ttl, long speed, Response[] res,
                                    byte[] clientGUID, 
                                    boolean busy, boolean uploaded, 
                                    boolean measuredSpeed, 
                                    boolean isFromMcast) {
        return new LinkedList();
    }    
}
