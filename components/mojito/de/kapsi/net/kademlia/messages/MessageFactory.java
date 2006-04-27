/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package de.kapsi.net.kademlia.messages;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.request.FindNodeRequest;
import de.kapsi.net.kademlia.messages.request.FindValueRequest;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.request.StatsRequest;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.messages.response.PingResponse;
import de.kapsi.net.kademlia.messages.response.StatsResponse;
import de.kapsi.net.kademlia.messages.response.StoreResponse;
import de.kapsi.net.kademlia.security.QueryKey;

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
    
    private ContactNode getLocalNode() {
        return context.getLocalNode();
    }
    
    private KUID createMessageID() {
        return KUID.createRandomMessageID();
    }
    
    private int getEstimatedSize() {
        return context.size();
    }
    
    public PingRequest createPingRequest() {
        return new PingRequest(getVendor(), getVersion(), getLocalNode(), createMessageID());
    }
    
    public PingResponse createPingResponse(KUID messageId, SocketAddress address) {
        return new PingResponse(getVendor(), getVersion(), getLocalNode(), messageId, address, getEstimatedSize());
    }
    
    public FindNodeRequest createFindNodeRequest(KUID lookup) {
        return new FindNodeRequest(getVendor(), getVersion(), getLocalNode(), createMessageID(), lookup);
    }
    
    public FindNodeResponse createFindNodeResponse(KUID messageId, QueryKey queryKey, List nodes) {
        return new FindNodeResponse(getVendor(), getVersion(), getLocalNode(), messageId, queryKey, nodes);
    }
    
    public FindValueRequest createFindValueRequest(KUID lookup) {
        return new FindValueRequest(getVendor(), getVersion(), getLocalNode(), createMessageID(), lookup);
    }
    
    public FindValueResponse createFindValueResponse(KUID messageId, Collection values) {
        return new FindValueResponse(getVendor(), getVersion(), getLocalNode(), messageId, values);
    }
    
    public StoreRequest createStoreRequest(int remaining, QueryKey queryKey, Collection values) {
        return new StoreRequest(getVendor(), getVersion(), getLocalNode(), createMessageID(), remaining, queryKey, values);
    }
    
    public StoreResponse createStoreResponse(KUID messageId, int requesting, Collection status) {
        return new StoreResponse(getVendor(), getVersion(), getLocalNode(), messageId, requesting, status);
    }
    
    public StatsRequest createStatsRequest(KUID messageId, byte[] signature, int request) {
        return new StatsRequest(getVendor(), getVersion(), getLocalNode(), messageId, signature, request);
    }

    public StatsResponse createStatsResponse(KUID messageId, String statistics) {
        return new StatsResponse(getVendor(), getVersion(), getLocalNode(), messageId, statistics);
    }
}
