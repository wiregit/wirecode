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

/** A stub for MessageRouter that does nothing. */
@SuppressWarnings("unchecked")
public class MessageRouterStub extends MessageRouter {
    
    @Override
    public void downloadFinished(GUID guid) throws IllegalArgumentException {
    }

    @Override
    protected boolean respondToQueryRequest(QueryRequest queryRequest,
                                            byte[] clientGUID,
                                            ReplyHandler handler) {
        return false;
    }

    @Override
    protected void respondToPingRequest(PingRequest request,
                                        ReplyHandler handler) {
	}

    @Override
    protected void respondToUDPPingRequest(PingRequest request, 
                                           InetSocketAddress addr,
                                           ReplyHandler handler) {}

    @Override
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
}
