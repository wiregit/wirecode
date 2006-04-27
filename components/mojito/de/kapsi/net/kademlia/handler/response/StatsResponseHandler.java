package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.StatsListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.StatsResponse;

public class StatsResponseHandler extends AbstractResponseHandler {

    private static final Log LOG = LogFactory.getLog(StatsResponseHandler.class);
    
    private StatsListener l;
    
    public StatsResponseHandler(Context context, StatsListener l) {
        super(context);
        this.l = l;
    }

    public void handleResponse(ResponseMessage message, final long time) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stats request to " + message.getContactNode() + " succeeded");
        }
        
        final StatsResponse response = (StatsResponse) message;
        
        if (l != null) {
            context.fireEvent(new Runnable() {
                public void run() {
                    l.nodeStatsResponse(response.getContactNode(), response.getStatistics(), time);
                }
            });
        }
    }

    public void handleTimeout(final KUID nodeId, final SocketAddress dst, final long time) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stats request to " + ContactNode.toString(nodeId, dst) 
                    + " failed");
        }
        
        if (l != null) {
            context.fireEvent(new Runnable() {
                public void run() {
                    l.nodeStatsTimeout(nodeId, dst);
                }
            });
        }
    }

    
}
