package de.kapsi.net.kademlia.messages.request;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.RequestMessage;

public class StatsRequest extends RequestMessage {
    
    public static final int STATS = 0x00;
    public static final int DB = 0x01;
    public static final int RT = 0x02;
    
    private int request;

    public StatsRequest(int vendor, int version, 
            ContactNode node, KUID messageId, byte[] signature, int request) {
        super(vendor, version, node, messageId, signature);
        
        this.request = request;
    }
    
    public int getRequest() {
        return request;
    }

    public boolean isDBRequest() {
        return (request & DB) == DB;
    }
    
    public boolean isRTRequest() {
        return (request & RT) == RT;
    }
    
}
