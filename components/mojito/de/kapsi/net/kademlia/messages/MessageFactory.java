/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.request.FindNodeRequest;
import de.kapsi.net.kademlia.messages.request.FindValueRequest;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.messages.response.PingResponse;
import de.kapsi.net.kademlia.messages.response.StoreResponse;

public class MessageFactory {
    
    protected final Context context;
    
    public MessageFactory(Context context) {
        this.context = context;
    }
    
    private int getVendor() {
        return context.getVendor();
    }
    
    private int getVersion() {
        return context.getVersion();
    }
    
    private KUID getLocalNodeID() {
        return context.getLocalNodeID();
    }
    
    private KUID createMessageID() {
        return KUID.createRandomMessageID();
    }
    
    public PingRequest createPingRequest() {
        return new PingRequest(getVendor(), getVersion(), getLocalNodeID(), createMessageID());
    }
    
    public PingResponse createPingResponse(KUID messageId, SocketAddress address) {
        return new PingResponse(getVendor(), getVersion(), getLocalNodeID(), messageId, address);
    }
    
    public FindNodeRequest createFindNodeRequest(KUID lookup) {
        return new FindNodeRequest(getVendor(), getVersion(), getLocalNodeID(), createMessageID(), lookup);
    }
    
    public FindNodeResponse createFindNodeResponse(KUID messageId, List nodes) {
        return new FindNodeResponse(getVendor(), getVersion(), getLocalNodeID(), messageId, nodes);
    }
    
    public FindValueRequest createFindValueRequest(KUID lookup) {
        return new FindValueRequest(getVendor(), getVersion(), getLocalNodeID(), createMessageID(), lookup);
    }
    
    public FindValueResponse createFindValueResponse(KUID messageId, Collection values) {
        return new FindValueResponse(getVendor(), getVersion(), getLocalNodeID(), messageId, values);
    }
    
    public StoreRequest createStoreRequest(Collection values) {
        return new StoreRequest(getVendor(), getVersion(), getLocalNodeID(), createMessageID(), values);
    }
    
    public StoreResponse createStoreResponse(KUID messageId, Collection status) {
        return new StoreResponse(getVendor(), getVersion(), getLocalNodeID(), messageId, status);
    }
}
