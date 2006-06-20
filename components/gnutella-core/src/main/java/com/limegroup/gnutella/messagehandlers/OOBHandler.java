package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;

public class OOBHandler implements MessageHandler, Runnable {
	
	private final MessageRouter router;
	
    private final Map<OOBSession, Integer> OOBSessions = 
    	Collections.synchronizedMap(new HashMap<OOBSession, Integer>(1000));
    
	public OOBHandler(MessageRouter router) {
		this.router = router;
	}

	public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
		if (msg instanceof ReplyNumberVendorMessage)
			handleRNVM((ReplyNumberVendorMessage)msg, handler);
		else if (msg instanceof QueryReply)
			handleOOBReply((QueryReply)msg, handler);
		else 
			throw new IllegalArgumentException("can't handle this type of message");
	}
	
	private void handleRNVM(ReplyNumberVendorMessage msg, ReplyHandler handler) {
		GUID g = new GUID(msg.getGUID());
		OOBSession session = new OOBSession(
				g,
				handler.getInetAddress(),
				handler.getPort());
		
		int toRequest = router.getNumOOBToRequest(msg, handler);
		if (toRequest <= 0)
			return;
		
		LimeACKVendorMessage ack =
			new LimeACKVendorMessage(g, toRequest);
		synchronized(OOBSessions) {
			// remove is necessary to refresh the timestamp.
			Integer previous = OOBSessions.remove(session);
			if (previous == null)
				previous = toRequest;
			else
				previous += toRequest;
			OOBSessions.put(session, previous);
		}
		OutOfBandThroughputStat.RESPONSES_REQUESTED.addData(toRequest);
		handler.reply(ack);
	}
	
	private void handleOOBReply(QueryReply reply, ReplyHandler handler) {
        ReceivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(reply);
        // only account for OOB stuff if this was response to a 
        // OOB query, multicast stuff is sent over UDP too....
        
        int numResps = reply.getResultCount();
        
        if (!reply.isReplyToMulticastQuery())
            OutOfBandThroughputStat.RESPONSES_RECEIVED.addData(numResps);
        
        OOBSession session = new OOBSession(
        		new GUID(reply.getGUID()),
        		handler.getInetAddress(),
        		handler.getPort());
        
        Integer numRequested = OOBSessions.get(session);
        if (numRequested == null)
        	return;
        
        numRequested -= numResps;
        
        if (numRequested > 0)
        	OOBSessions.put(session, numRequested);
        else {
        	OOBSessions.remove(session);
        	if (numRequested < 0) // too many, ignore.
        		return;
        }
        
        router.handleQueryReply(reply, handler);
	}
	
	private class OOBSession {
    	private final GUID g;
    	private final InetAddress addr;
    	private final int port;
    	private final int hashCode;
    	private final long now;
    	OOBSession(GUID g, InetAddress addr, int port) {
    		this.g = g;
    		this.addr = addr;
    		this.port = port;
    		int hash = g.hashCode();
    		hash = 17 * hash + addr.hashCode();
    		hash = 17 * hash + port;
    		hashCode = hash;
    		now = System.currentTimeMillis();
    	}
    	
    	public int hashCode() {
    		return hashCode;
    	}
    	
    	public boolean equals(Object o) {
    		if (! (o instanceof OOBSession))
    			return false;
    		OOBSession other = (OOBSession) o;
    		return g.equals(other.g) && addr.equals(other.addr) && port == other.port;
    	}
    	
    	public boolean isExpired(long now) {
    		return now - this.now > router.getOOBExpireTime();
    	}
	}
	
	private void expire() {
		long now = System.currentTimeMillis();
		synchronized (OOBSessions) {
			for (Iterator<Map.Entry<OOBSession,Integer>> iter = 
				OOBSessions.entrySet().iterator();
			iter.hasNext();) {
				if (iter.next().getKey().isExpired(now))
					iter.remove();
			}
		}
	}
	
	public void run() {
		expire();
	}
}
