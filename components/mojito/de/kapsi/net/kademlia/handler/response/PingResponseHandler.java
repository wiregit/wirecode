package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Context.PingManager;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.PingResponse;

public class PingResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(PingResponseHandler.class);
    
    private NetworkStatisticContainer networkStats;
    
    private PingManager pingManager;
    
    public PingResponseHandler(Context context, PingManager pingManager) {
        super(context);
        
        this.pingManager = pingManager;
        networkStats = context.getNetworkStats();
    }

    public void handleResponse(KUID nodeId, 
            SocketAddress src, ResponseMessage message, long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received pong from " + ContactNode.toString(nodeId, src) 
                    + " after " + getErrors() + " errors and a total time of " + time() + "ms");
        }
        
        networkStats.PINGS_OK.incrementStat();
        
        PingResponse response = (PingResponse)message;
        context.setExternalSocketAddress(response.getSocketAddress());
        
        pingManager.handleSuccess(nodeId, src, time());
    }

    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage message, long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + ContactNode.toString(nodeId, dst) 
                    + " failed after " + time + "ms");
        }
        
        networkStats.PINGS_FAILED.incrementStat();
        super.handleTimeout(nodeId, dst, message, time);
    }
    
    protected void handleResend(KUID nodeId, SocketAddress dst, Message message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Re-sending Ping to " + ContactNode.toString(nodeId, dst));
        }
        
        networkStats.PINGS_SENT.incrementStat();
        super.handleResend(nodeId, dst, message);
    }

    protected void handleFinalTimeout(KUID nodeId, SocketAddress dst, Message message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Giving up to ping " + ContactNode.toString(nodeId, dst) 
                    + " after " + getMaxErrors() + " errors and a total time of "+ time() + "ms");
        }
        
        pingManager.handleTimeout(nodeId, dst, message, time());
    }
}
