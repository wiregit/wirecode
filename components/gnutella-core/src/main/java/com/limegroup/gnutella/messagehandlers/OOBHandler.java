package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.MessageSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.GUID;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.BypassedResultsCache;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;

/**
 * Handles {@link ReplyNumberVendorMessage} and {@link QueryReply} for 
 * out-of-band search results and manages a cache of session objects 
 * to keep track of the results that have alreay been received.
 */
@Singleton
public class OOBHandler implements MessageHandler, Runnable {
    
    private static final Log LOG = LogFactory.getLog(OOBHandler.class);
	
	private final MessageRouter router;
	
	private final MACCalculatorRepositoryManager MACCalculatorRepositoryManager;
    
    private final ScheduledExecutorService executor;
    
    private final OutOfBandStatistics outOfBandStatistics;
    
    private final NetworkInstanceUtils networkInstanceUtils;
	
    private final Map<Integer,OOBSession> OOBSessions =
        Collections.synchronizedMap(new HashMap<Integer, OOBSession>());
    
    @Inject
	public OOBHandler(MessageRouter router, 
            MACCalculatorRepositoryManager MACCalculatorRepositoryManager,
            @Named("backgroundExecutor") ScheduledExecutorService executor,
            OutOfBandStatistics outOfBandStatistics,
            NetworkInstanceUtils networkInstanceUtils) {
		this.router = router;
		this.MACCalculatorRepositoryManager = MACCalculatorRepositoryManager;
        this.executor = executor;
        this.outOfBandStatistics = outOfBandStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
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
	private void handleRNVM(ReplyNumberVendorMessage msg, final ReplyHandler handler) {
		GUID g = new GUID(msg.getGUID());

        if(LOG.isDebugEnabled()) {
            LOG.debug("Received RNVM from " + handler.getAddress() +
                    ":" + handler.getPort());
        }
        
        int toRequest;
        
        if(!router.isQueryAlive(g) ||
                (toRequest = router.getNumOOBToRequest(msg)) <= 0) {
            // remember as possible GUESS source though
            LOG.debug("Bypassing source");
            router.addBypassedSource(msg, handler);
            outOfBandStatistics.addBypassedResponse(msg.getNumResults());
            return;
        }
				
		LimeACKVendorMessage ack = null;
        if (msg.isOOBv3()) {
            SecurityToken t = new OOBSecurityToken(new OOBSecurityToken.OOBTokenData(handler, msg.getGUID(), toRequest), 
                    MACCalculatorRepositoryManager); 
            int hash = Arrays.hashCode(t.getBytes());
            synchronized(OOBSessions) {
                if (!OOBSessions.containsKey(hash)) {
                    OOBSessions.put(hash,new OOBSession(t, toRequest, new GUID(msg.getGUID()), true));
                    ack = new LimeACKVendorMessage(g, toRequest,t);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Sending OOBv3 LimeACK to " +
                                handler.getAddress() + ":" + handler.getPort());
                    }
                } else {
                    LOG.debug("RNVM has already been acked");
                }
            }
        } else {
            ack = new LimeACKVendorMessage(g, toRequest);
            if(LOG.isDebugEnabled()) {
                LOG.debug("Sending OOBv2 LimeACK to " +
                        handler.getAddress() + ":" + handler.getPort());
            }
        }
        
        if (ack != null) {
            outOfBandStatistics.addRequestedResponse(toRequest);
            handler.reply(ack);
            if (MessageSettings.OOB_REDUNDANCY.getValue()) {
                LOG.debug("Sending redundant LimeACK");
                final LimeACKVendorMessage ackf = ack;
                executor.schedule(new Runnable() {
                    public void run() {
                        handler.reply(ackf);
                    }
                }, 100, TimeUnit.MILLISECONDS);
            }
        }
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
        if(LOG.isDebugEnabled()) {
            LOG.debug("Handling OOB reply from " + handler.getAddress() +
                    ":" + handler.getPort());
        }
        
        // check if ip address of reply and sender of reply match
        // and update address of reply if necessary
        byte[] handlerAddress = handler.getInetAddress().getAddress(); 
        if (!Arrays.equals(handlerAddress, reply.getIPBytes())) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Reply has wrong address " + reply.getIP() +
                        ":" + reply.getPort());
            }
            // override address in packet
            try {
                // needs a push, we can update: works for fw-fw case and classic push
                // or not private, we can update
                if (reply.getNeedsPush() || !networkInstanceUtils.isPrivateAddress(reply.getIPBytes())) {
                    reply.setOOBAddress(handler.getInetAddress(), handler.getPort());
                }
                else {
                    // messed up case: doesn't want a push, but has a private address
                }
            }
            catch (BadPacketException bpe) {
                // invalid packet, don't handle it
                LOG.debug("Error overriding address");
                return;
            }
        }
        
        SecurityToken token = getVerifiedSecurityToken(reply, handler);
        if (token == null) {
            LOG.debug("Reply has no security token");
            if (!SearchSettings.DISABLE_OOB_V2.getBoolean()) {
                LOG.debug("Handling OOBv2 reply");
                router.handleQueryReply(reply, handler);
            }
            return;
        }
        
        int numResps = reply.getResultCount();
        outOfBandStatistics.addReceivedResponse(numResps);
        
        int requestedResponseCount = token.getBytes()[0] & 0xFF;
        
        /*
         * Router will handle the reply if it
         * it has a route && we still expect results for this OOB session
         */
        // if query is not of interest anymore return
        GUID queryGUID = new GUID(reply.getGUID());
        if (!router.isQueryAlive(queryGUID)) {
            router.addBypassedSource(reply, handler);
        }
        else {
            synchronized (OOBSessions) {
                int hashKey = Arrays.hashCode(token.getBytes());
                OOBSession session = OOBSessions.get(hashKey);
                if (session == null) {
                    session = new OOBSession(token, requestedResponseCount, queryGUID, false);
                    OOBSessions.put(hashKey, session);
                }

                int remaining = session.getRemainingResultsCount() - numResps;
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Reply has " + numResps + " results, " +
                            remaining + " remaining");
                }
                if(remaining >= 0) {
                    // parsing of query reply already done here in message dispatcher thread
                    try {
                        int added = session.countAddedResponses(reply.getResultsArray());
                        if(LOG.isDebugEnabled())
                            LOG.debug("Reply has " + added + " new results");                        
                        if(added > 0) {
                            LOG.debug("Handling OOBv3 reply");
                            router.handleQueryReply(reply, handler);
                        }
                    } 
                    catch (BadPacketException e) {
                        LOG.debug("Error getting results");
                        // ignore packet
                    }
                }
            }
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
            OOBSecurityToken oobKey = new OOBSecurityToken(securityBytes, MACCalculatorRepositoryManager);
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

    @InspectableContainer
    @SuppressWarnings("unused")
	private class OOBInspectable  {

        @InspectionPoint("oob sessions")
        public final Inspectable oobSessions = new Inspectable(){
            public Object inspect() {
                List<Object> list;
                synchronized (OOBSessions) {
                    list = new ArrayList<Object>(OOBSessions.size());
                    for (OOBSession o : OOBSessions.values()) 
                        list.add(o.inspect());
                }
                return list;
            }
        };
	}
}
