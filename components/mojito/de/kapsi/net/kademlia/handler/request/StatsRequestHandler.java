package de.kapsi.net.kademlia.handler.request;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.request.StatsRequest;
import de.kapsi.net.kademlia.messages.response.StatsResponse;

public class StatsRequestHandler extends AbstractRequestHandler {

    private static final Log LOG = LogFactory.getLog(StatsRequestHandler.class);
    
    private final NetworkStatisticContainer networkStats;
    
    public StatsRequestHandler(Context context) {
        super(context);
        networkStats = context.getNetworkStats();
    }

    public void handleRequest(RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(message.getContactNode() + " sent us a Stats Request");
        }
        
        networkStats.STATS_REQUEST.incrementStat();
        
        StatsRequest req = (StatsRequest) message;
        StringWriter writer = new StringWriter();
        
        if(req.isDBRequest()) {
            context.getDHTStats().dumpDataBase(writer);
        } else if (req.isRTRequest()){
            context.getDHTStats().dumpRouteTable(writer);
        } else {
            context.getDHTStats().dumpStats(writer, false);
        }
        
        StatsResponse response = context.getMessageFactory()
            .createStatsResponse(message.getMessageID(),writer.toString());
        
        context.getMessageDispatcher().send(message.getContactNode(), response, null);
    }
}
