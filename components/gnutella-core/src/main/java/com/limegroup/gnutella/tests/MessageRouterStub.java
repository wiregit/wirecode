package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.routing.*;

/** A stub for MessageRouter that does nothing. */
public class MessageRouterStub extends MessageRouter {

    protected  void respondToPingRequest(PingRequest pingRequest,
                                                 Acceptor acceptor) { 
    }


    protected  void respondToQueryRequest(QueryRequest queryRequest,
                                                  Acceptor acceptor,
                                                  byte[] clientGUID) {
    }

    protected  void handlePingReplyForMe(
        PingReply pingReply,
        ManagedConnection receivingConnection) {
    }

    protected  void handleQueryReplyForMe(
        QueryReply queryReply,
        ManagedConnection receivingConnection) {
    }


    protected  void handlePushRequestForMe(
        PushRequest pushRequest,
        ManagedConnection receivingConnection) {
    }
    
    protected void addQueryRoutingEntries(QueryRouteTable qrt)
    {
    }    

    public GroupPingRequest createGroupPingRequest(String group) {
        return new GroupPingRequest((byte)5, 6346, new byte[0], 0l, 0l, "");
    }
     
}
