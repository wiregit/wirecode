package com.limegroup.gnutella.stubs;


import java.net.InetAddress;
import java.net.UnknownHostException;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.sun.java.util.collections.*;
import java.net.*;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.sun.java.util.collections.Set;

/**
 * Stub for the <tt>ReplyHandler</tt> interface.
 */
public class ReplyHandlerStub implements ReplyHandler {
    
    public boolean isOpen() {
        return true;
    }
	public void send(Message m) {
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

    public String getLocalePref() {
        return "en";
    }

    public void handleUDPCrawlerPong(UDPCrawlerPong pong){}

}








