package de.kapsi.net.kademlia.messages.response;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public class StatsResponse extends ResponseMessage{
    
    protected final String statistics;

    public StatsResponse(int vendor, int version, ContactNode node, 
            KUID messageId, String statistics) {
        super(vendor, version, node, messageId);
        
        this.statistics = statistics;
    }
    
    public String getStatistics() {
        return statistics;
    }

}
