package com.limegroup.gnutella.stubs;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.limewire.security.SecurityToken;

import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HackMessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;

/** A stub for MessageRouter that does nothing. */
@SuppressWarnings("unchecked")
@Singleton
public class MessageRouterStub extends HackMessageRouter {
    
    
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
                                    SecurityToken securityToken) {
        return new LinkedList();
    }
}
