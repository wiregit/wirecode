package de.kapsi.net.kademlia.messages.response;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public class StatsResponse extends ResponseMessage{
    
    protected final String statistics;

    public StatsResponse(int vendor, int version, KUID nodeId, 
            KUID messageId, String statistics) {
        super(vendor, version, nodeId, messageId);
        
        this.statistics = statistics;
    }
    
    public String getStatistics() {
        return statistics;
    }

}
