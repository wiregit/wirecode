package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*; 
import com.limegroup.gnutella.routing.*;
import java.net.DatagramPacket;

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

	public GroupPingRequest createGroupPingRequest(String group) {
		return new GroupPingRequest((byte)5, 6346, new byte[0], 0l, 0l, "");
	}
}
