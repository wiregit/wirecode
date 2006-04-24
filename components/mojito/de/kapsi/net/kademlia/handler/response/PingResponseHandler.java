package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Context.PingContext;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.response.PingResponse;

public class PingResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(PingResponseHandler.class);
    
    private NetworkStatisticContainer networkStats;
    
    private PingContext pingContext;
    
    public PingResponseHandler(Context context, PingContext pingContext) {
        super(context);
        
        this.pingContext = pingContext;
        networkStats = context.getNetworkStats();
    }

    public void handleResponse(KUID nodeId, 
            SocketAddress src, Message message, long time) throws IOException {
        
        networkStats.PINGS_OK.incrementStat();
        
        PingResponse response = (PingResponse)message;
        context.setExternalSocketAddress(response.getSocketAddress());
        
        pingContext.handleSuccess(nodeId, src, time());
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, Message message, long time) throws IOException {
        networkStats.PINGS_FAILED.incrementStat();
        super.handleTimeout(nodeId, dst, message, time);
    }
    
    protected void handleResend(KUID nodeId, SocketAddress dst, Message message) throws IOException {
        networkStats.PINGS_SENT.incrementStat();
        super.handleResend(nodeId, dst, message);
    }

    protected void handleFinalTimeout(KUID nodeId, SocketAddress dst, Message message) throws IOException {
        pingContext.handleTimeout(nodeId, dst, message, time());
    }
}
