package com.limegroup.gnutella.stubs;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;

/** A stub for MessageRouter that does nothing. */
public class MessageRouterStub extends MessageRouter {
    
    private static final byte [] MYGUID = GUID.makeGuid();

    protected boolean respondToQueryRequest(QueryRequest queryRequest,
                                            byte[] clientGUID,
                                            ReplyHandler handler) {
        return false;
    }

    
    protected void addQueryRoutingEntries(QueryRouteTable qrt) {
    }    

    protected void respondToPingRequest(PingRequest request,
                                        ReplyHandler handler) {
	}

    protected void respondToUDPPingRequest(PingRequest request, 
                                           InetSocketAddress addr,
                                           ReplyHandler handler) {}

    protected List createQueryReply(byte[] guid, byte ttl,
                                    long speed, 
                                    Response[] res, byte[] clientGUID, 
                                    boolean busy, 
                                    boolean uploaded, 
                                    boolean measuredSpeed, 
                                    boolean isFromMcast,
                                    boolean shouldMarkForFWTransfer,
                                    byte [] securityToken) {
        return new LinkedList();
    }
    
    /**
     * the proper constructor may not initialize our guid, 
     * so we create a fake one
     */
    public byte []getOurGUID() {
        return MYGUID;
    }
}
