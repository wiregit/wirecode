package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.sun.java.util.collections.*;
import java.net.*;

/**
 * Stub for the <tt>ReplyHandler</tt> interface.
 */
public class ReplyHandlerStub implements ReplyHandler {
    
    public boolean isOpen() {
        return true;
    }
    public void handlePingReply(PingReply pingReply, 
                                ReplyHandler receivingConnection) {
    }
    public void handlePushRequest(PushRequest pushRequest, 
                                  ReplyHandler receivingConnection) {
    }
    public void handleQueryReply(QueryReply queryReply, 
                                 ReplyHandler receivingConnection) {
    }
    public int getNumMessagesReceived() {
        return 0;
    }
    public void countDroppedMessage() {
    }
    public Set getDomains() {
        return null;
    }
    public boolean isPersonalSpam(Message m) {
        return false;
    }
    public boolean isOutgoing() {
        return false;
    }
    public boolean isKillable() {
        return false;
    }
    public boolean isSupernodeClientConnection() {
        return false;
    }
    public boolean isLeafConnection() {
        return false;
    }
    public boolean isHighDegreeConnection() {
        return false;
    }
    
    public boolean isUltrapeerQueryRoutingConnection() {
        return false;
    }
    
    public boolean isGoodUltrapeer() {
        return false;
    }

    public boolean isGoodLeaf() {
        return false;
    }

    public boolean supportsPongCaching() {
        return true;
    }

    public boolean allowNewPings() {
        return true;
    }

    public void updatePingTime() {
    }
    
    public InetAddress getInetAddress() {
        try {
            return InetAddress.getByName("30.24.0.5");
        } catch(UnknownHostException e) {
            // should NEVER happen
            e.printStackTrace();
            return null;
        }
    }

    public void handleStatisticVM(StatisticVendorMessage svm) { }

    public boolean isStable() {
        return true;
    }
}








