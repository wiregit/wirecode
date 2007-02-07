package com.limegroup.gnutella.messagehandlers;


import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntSet;
import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.SecurityToken;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;

public class OOBHandler implements MessageHandler, Runnable {
    
    private static final Log LOG = LogFactory.getLog(OOBHandler.class);
	
	private final MessageRouter router;
	
    private final Map<OOBSession, OOBSession> OOBSessions =
        Collections.synchronizedMap(new HashMap<OOBSession, OOBSession>());
    
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
                    new OOBQueryKey(new OOBTokenData(handler, msg.getGUID(), toRequest)));
        
		OutOfBandThroughputStat.RESPONSES_REQUESTED.addData(toRequest);
		handler.reply(ack);
	}
    
    private void handleOOBReply(QueryReply reply, ReplyHandler handler) {
        if(LOG.isTraceEnabled())
            LOG.trace("Handling reply: " + reply + ", from: " + handler);
        
        // if query is not of interest anymore return
        GUID queryGUID = new GUID(reply.getGUID());
        
        SecurityToken token = getVerifiedSecurityToken(reply, handler);
        if (token == null) {
            LOG.trace("Didn't request any OOB replies for this GUID from host");
            // TODO spammer, remember them
            return;
        }
        
        ReceivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(reply);
        
        int numResps = reply.getResultCount();
        OutOfBandThroughputStat.RESPONSES_RECEIVED.addData(numResps);
        
        int requestedResponseCount = token.getBytes()[0] & 0xFF;
        OOBSession session = new OOBSession(token, requestedResponseCount, queryGUID);
            
        // Allow the router to handle the query reply in the
        // following scenarios:
        // a) We sent a Reply# message requesting the results,
        //    and it sent back <= the number of results we
        //    wanted.
        // b) We sent a directed unicast query to that host
        //    using this specific query GUID.
        
        // synchronize over everything so sessions are not expired
        // from another thread while we work on them, we could
        // use a lock for more finegrained synchronization if need be
        synchronized (OOBSessions) {
            session = getCanonicalSession(session);
            int remainingCount = session.getRemainingResultsCount() - numResps; 
            if (remainingCount >= 0) {
                if(LOG.isTraceEnabled())
                    LOG.trace("Requested more than got (" + remainingCount + " left over)");
                // parsing of query reply already done here in message dispatcher thread
                try {
                    int added = session.countAddedResponses(reply.getResultsArray());
                    if (added > 0) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Handling the reply.");
                        }
                        router.handleQueryReply(reply, handler);
                    }
                } 
                catch (BadPacketException e) {
                    // ignore packet
                }
            }
        }
	}
    
    private final OOBSession getCanonicalSession(OOBSession session) {
        OOBSession existing = OOBSessions.get(session);
        if (existing == null) {
            existing = session;
            OOBSessions.put(existing, existing);
        }
        return existing;
    }
	
	private SecurityToken getVerifiedSecurityToken(QueryReply reply, ReplyHandler handler) {
	    byte[] securityBytes = reply.getSecurityToken();
        if (securityBytes == null) {
            return null;
        }

        try {
            OOBQueryKey oobKey = new OOBQueryKey(securityBytes);
            OOBTokenData data = new OOBTokenData(handler, reply.getGUID(), securityBytes[0] & 0xFF);
            if (oobKey.isFor(data)) {
                return oobKey;
            }
        }
        catch (InvalidSecurityTokenException e) {
            // invalid security token echoed back
        }
        return null;
	}

    private class OOBSession {
    	
        private final SecurityToken token;
        
        private final int hashCode;
        
        private final IntSet responseHashCodes;
        
        private int responseCount = 0;
        
        private final int requestedResponseCount;
        
        private final GUID guid;
    	
        public OOBSession(SecurityToken token, int requestedResponseCount, GUID guid) {
            this.token = token;
            this.hashCode = Arrays.hashCode(token.getBytes());
            this.requestedResponseCount = requestedResponseCount;
            this.responseHashCodes = new IntSet(requestedResponseCount);
            this.guid = guid;
    	}
    	
        public int hashCode() {
    		return hashCode;
    	}
    	
        /**
         * Counts the responses uniquely. 
         */
        public int countAddedResponses(Response[] responses) {
            int added = 0;
            for (Response response : responses) {
                ++responseCount;
                Set<URN> urns = response.getUrns();
                if (!urns.isEmpty()) {
                    added += responseHashCodes.add(urns.iterator().next().hashCode()) ? 1 : 0;
                }
                else {
                    added += responseHashCodes.add(response.hashCode()) ? 1 : 0;
                }
            }
            return added;
        }
        
        public final int getRemainingResultsCount() {
            return requestedResponseCount - responseHashCodes.size();
        }
        
    	public boolean equals(Object o) {
    		if (! (o instanceof OOBSession))
    			return false;
    		OOBSession other = (OOBSession) o;
    		return Arrays.equals(token.getBytes(), other.token.getBytes());
    	}
    	
        public boolean queryIsAlive() {
            return router.isQueryAlive(guid);
        }
	}
	
	private void expire() {
		synchronized (OOBSessions) {
			for (Iterator<Map.Entry<OOBSession, OOBSession>> iter = 
			    OOBSessions.entrySet().iterator(); iter.hasNext();) {
			    if (!iter.next().getKey().queryIsAlive())
			        iter.remove();
			}
		}
	}
	
	public void run() {
		expire();
	}
}
