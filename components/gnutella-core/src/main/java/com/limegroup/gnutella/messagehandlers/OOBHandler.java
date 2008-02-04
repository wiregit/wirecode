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
import org.limewire.io.NetworkUtils;
import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.SecurityToken;

import com.limegroup.gnutella.BypassedResultsCache;
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
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;

/**
 * Handles {@link ReplyNumberVendorMessage} and {@link QueryReply} for 
 * out-of-band search results and manages a cache of session objects 
 * to keep track of the results that have alreay been received.
 */
public class OOBHandler implements MessageHandler, Runnable {
    
    private static final Log LOG = LogFactory.getLog(OOBHandler.class);
	
	private final MessageRouter router;
	
    private final Map<Integer,OOBSession> OOBSessions =
        Collections.synchronizedMap(new HashMap<Integer, OOBSession>());
    
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
	
    /**
     * Handles the reply number message, verifying the query for it is still alive
     * and more results are wanted and sending a {@link LimeACKVendorMessage} in
     * that case. Otherwise the source of the <code>msg</code> is added to the 
     * {@link BypassedResultsCache}.
     */
	private void handleRNVM(ReplyNumberVendorMessage msg, ReplyHandler handler) {
		GUID g = new GUID(msg.getGUID());

        int toRequest;
        
        if (!router.isQueryAlive(g) || (toRequest = router.getNumOOBToRequest(msg)) <= 0) {
            // remember as possible GUESS source though
            router.addBypassedSource(msg, handler);
            OutOfBandThroughputStat.RESPONSES_BYPASSED.addData(msg.getNumResults());
            return;
        }
				
		LimeACKVendorMessage ack;
        if (msg.isOOBv3()) {
			ack = new LimeACKVendorMessage(g, toRequest, 
                    new OOBSecurityToken(new OOBSecurityToken.OOBTokenData(handler, msg.getGUID(), toRequest)));
        } else
            ack = new LimeACKVendorMessage(g, toRequest);
        
		OutOfBandThroughputStat.RESPONSES_REQUESTED.addData(toRequest);
		handler.reply(ack);
	}
    
    /**
     * Handles an out-of-band query reply verifying if its security token is valid
     * and creating a session object that keeps track of the number of results
     * received for that security token.
     * 
     * Invalid messages with invalid security token or without token or duplicate
     * messages are ignored.
     * 
     * If the query is not alive messages are discarded and added to the
     *  {@link BypassedResultsCache}.
     */
    private void handleOOBReply(QueryReply reply, ReplyHandler handler) {
        if(LOG.isTraceEnabled())
            LOG.trace("Handling reply: " + reply + ", from: " + handler);
        
        // check if ip address of reply and sender of reply match
        // and update address of reply if necessary
        byte[] handlerAddress = handler.getInetAddress().getAddress(); 
        if (!Arrays.equals(handlerAddress, reply.getIPBytes())) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("IP addresses of sender and packet did not match, sender: " + handler.getInetAddress()
                        + ", packet: " + reply.getIP());
            }
            // override address in packet
            try {
                // needs a push, we can update: works for fw-fw case and classic push
                // or not private, we can update
                if (reply.getNeedsPush() || !NetworkUtils.isPrivateAddress(reply.getIPBytes())) {
                    reply.setOOBAddress(handler.getInetAddress(), handler.getPort());
                }
                else {
                    // messed up case: doesn't want a push, but has a private address
                }
            }
            catch (BadPacketException bpe) {
                // invalid packet, don't handle it
                return;
            }
        }
        
        SecurityToken token = getVerifiedSecurityToken(reply, handler);
        if (token == null) {
            if (!SearchSettings.DISABLE_OOB_V2.getBoolean()) 
                router.handleQueryReply(reply, handler);
            return;
        }

        // from here on, it's OOBv3 specific code
        ReceivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(reply);
        
        int numResps = reply.getResultCount();
        OutOfBandThroughputStat.RESPONSES_RECEIVED.addData(numResps);
        
        int requestedResponseCount = token.getBytes()[0] & 0xFF;
        
        
        boolean shouldAddBypassedSource = false;
        /*
         * Router will handle the reply if it
         * it has a route && we still expect results for this OOB session
         */
        synchronized (OOBSessions) {
            // if query is not of interest anymore return
            GUID queryGUID = new GUID(reply.getGUID());
            if (!router.isQueryAlive(queryGUID)) {
                shouldAddBypassedSource = true;
            }
            else {
                int hashKey = Arrays.hashCode(token.getBytes());
                OOBSession session = OOBSessions.get(hashKey);
                if (session == null) {
                    session = new OOBSession(token, requestedResponseCount, queryGUID);
                    OOBSessions.put(hashKey, session);
                }
                
                int remainingCount = session.getRemainingResultsCount() - numResps; 
                if (remainingCount >= 0) {
                    if(LOG.isTraceEnabled())
                        LOG.trace("Requested >= than got (" + remainingCount + " left over)");
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
        
        if (shouldAddBypassedSource) {
            router.addBypassedSource(reply, handler);
        }
    }
    
    /**
     * Reconstructs the security token from the query reply and verifies it
     * against the handler, the number of results requested and the GUID of
     * the reply.
     *
     * @return null if there is no security token in the reply or the security
     * token did not validate
     */
	private SecurityToken getVerifiedSecurityToken(QueryReply reply, ReplyHandler handler) {
	    byte[] securityBytes = reply.getSecurityToken();
        if (securityBytes == null) {
            return null;
        }

        try {
            OOBSecurityToken oobKey = new OOBSecurityToken(securityBytes);
            OOBSecurityToken.OOBTokenData data = 
                new OOBSecurityToken.OOBTokenData(handler, reply.getGUID(), securityBytes[0] & 0xFF);
            if (oobKey.isFor(data)) {
                return oobKey;
            }
        }
        catch (InvalidSecurityTokenException e) {
            // invalid security token echoed back
        }
        return null;
	}

    /**
     * Keeps track of how many results have been received for a security
     * token. 
     */
    private class OOBSession {
    	
        private final SecurityToken token;
        private final IntSet urnHashCodes;
        
        private IntSet responseHashCodes;
        
        private final int requestedResponseCount;
        private final GUID guid;
    	
        public OOBSession(SecurityToken token, int requestedResponseCount, GUID guid) {
            this.token = token;
            this.requestedResponseCount = requestedResponseCount;
            this.urnHashCodes = new IntSet(requestedResponseCount);
            this.guid = guid;
    	}
        
        GUID getGUID() {
            return guid;
        }
    	
        /**
         * Counts the responses uniquely. 
         */
        public int countAddedResponses(Response[] responses) {
            int added = 0;
            for (Response response : responses) {
                Set<URN> urns = response.getUrns();
                if (!urns.isEmpty()) {
                    added += urnHashCodes.add(urns.iterator().next().hashCode()) ? 1 : 0;
                }
                else {
                    // create lazily since responses should have urns
                    if (responseHashCodes == null) {
                        responseHashCodes = new IntSet();
                    }
                    added += responseHashCodes.add(response.hashCode()) ? 1 : 0;
                }
            }
            return added;
        }
        
        /**
         * Returns the number of results that are still expected to come in.
         */
        public final int getRemainingResultsCount() {
            return requestedResponseCount - urnHashCodes.size() - (responseHashCodes != null ? responseHashCodes.size() : 0); 
        }
        
    	public boolean equals(Object o) {
    		if (! (o instanceof OOBSession))
    			return false;
    		OOBSession other = (OOBSession) o;
    		return Arrays.equals(token.getBytes(), other.token.getBytes());
    	}
	}
	
	private void expire() {
		synchronized (OOBSessions) {
			for (Iterator<Map.Entry<Integer, OOBSession>> iter = 
			    OOBSessions.entrySet().iterator(); iter.hasNext();) {
			    if (!router.isQueryAlive(iter.next().getValue().getGUID()))
			        iter.remove();
			}
		}
	}
	
	public void run() {
		expire();
	}
}
