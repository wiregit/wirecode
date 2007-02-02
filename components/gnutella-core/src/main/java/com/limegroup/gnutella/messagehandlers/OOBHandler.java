package com.limegroup.gnutella.messagehandlers;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IpPort;
import org.limewire.security.QueryKey;
import org.limewire.security.SecurityToken;
import org.limewire.service.ErrorService;

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
    
    private static final Log LOG = LogFactory.getLog(OOBHandler.class);
	
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
	
		int toRequest = router.getNumOOBToRequest(msg, handler);
		if (toRequest <= 0)
			return;
		
		LimeACKVendorMessage ack =
			new LimeACKVendorMessage(g, toRequest, 
                    QueryKey.getQueryKey(createMessageKeyData(handler, msg.getGUID(), toRequest)));
        
		OutOfBandThroughputStat.RESPONSES_REQUESTED.addData(toRequest);
		handler.reply(ack);
	}
    
    private static byte[] createMessageKeyData(ReplyHandler replyHandler, byte[] guid, int requestNum) {
        if (requestNum <= 0 || requestNum > 255) {
            throw new IllegalArgumentException("requestNum to small or too large " + requestNum);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(23);
        DataOutputStream data = new DataOutputStream(baos);
        try {
            data.writeInt(replyHandler.getPort());
            // TODO fberger convert to ipv6
            data.write(replyHandler.getInetAddress().getAddress());
            data.writeShort(requestNum);
            data.write(guid);
        }
        catch (IOException ie) {
            ErrorService.error(ie);
        }
        return baos.toByteArray();
    }
	
	private void handleOOBReply(QueryReply reply, ReplyHandler handler) {
        if(LOG.isTraceEnabled())
            LOG.trace("Handling reply: " + reply + ", from: " + handler);
        
        // only account for OOB stuff if this was response to a 
        // OOB query, multicast stuff is sent over UDP too....
        if (reply.isReplyToMulticastQuery()) {
            return;
        }
        
        if (!verifyReply(reply, handler)) {
            LOG.trace("Didn't request any OOB replies for this GUID from host");
            // TODO fberger spammer, handle somehow
            return;
        }
        
        ReceivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(reply);
        
        int numResps = reply.getResultCount();
        OutOfBandThroughputStat.RESPONSES_RECEIVED.addData(numResps);
        GUID guid = new GUID(reply.getGUID());
        
        OOBSession session = new OOBSession(guid, handler.getInetAddress(),
                handler.getPort());
            
        // Allow the router to handle the query reply in the
        // following scenarios:
        // a) We sent a Reply# message requesting the results,
        //    and it sent back <= the number of results we
        //    wanted.
        // b) We sent a directed unicast query to that host
        //    using this specific query GUID.
        Integer numRequested = OOBSessions.get(session);
        if (numRequested == null) {
            if(!router.isHostUnicastQueried(guid, session)) {
                LOG.trace("Didn't directly unicast this host with this GUID");
                return;
            }
        } 
        else {
            numRequested -= numResps;
            if (numRequested > 0) {
                if(LOG.isTraceEnabled())
                    LOG.trace("Requested more than got (" + numRequested + " left over)");
                OOBSessions.put(session, numRequested);
            } else {
                OOBSessions.remove(session);
                if (numRequested < 0) { // too many, ignore.
                    if(LOG.isTraceEnabled())
                        LOG.trace("Received more than requested (by" + (-numRequested) + ")");
                    if(!router.isHostUnicastQueried(guid, session)) {
                        LOG.trace("Didn't directly unicast this host with this GUID");
                        return;
                    }
                }
            }
        }
        
        LOG.trace("Handling the reply.");
        router.handleQueryReply(reply, handler);
	}
	
	private boolean verifyReply(QueryReply reply, ReplyHandler handler) {
	    SecurityToken securitToken = reply.getSecurityToken();
        if (securitToken== null) {
            return false;
        }
        
        // TODO fberger
        return false;
	}

    private class OOBSession implements IpPort {
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

        public String getAddress() {
            return addr.getHostAddress();
        }

        public InetAddress getInetAddress() {
            return addr;
        }

        public int getPort() {
            return port;
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
