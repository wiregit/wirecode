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
    
    private KUID createMessageID(SocketAddress dst) {
        return KUID.createRandomMessageID(dst);
    }
    
    private int getEstimatedSize() {
        return context.size();
    }
    
    public PingRequest createPingRequest(SocketAddress dst) {
        return new PingRequest(getVendor(), getVersion(), getLocalNode(), createMessageID(dst));
    }
    
    public PingResponse createPingResponse(RequestMessage request, SocketAddress externalAddress) {
        if (context.getSocketAddress().equals(externalAddress)) {
            throw new IllegalArgumentException("Cannot tell other Node that its external address is the same as yours!");
        }
        return new PingResponse(getVendor(), getVersion(), getLocalNode(), request.getMessageID(), externalAddress, getEstimatedSize());
    }
    
    public FindNodeRequest createFindNodeRequest(SocketAddress dst, KUID lookup) {
        if (!lookup.isNodeID()) {
            throw new IllegalArgumentException();
        }
        return new FindNodeRequest(getVendor(), getVersion(), getLocalNode(), createMessageID(dst), lookup);
    }
    
    public FindNodeResponse createFindNodeResponse(RequestMessage request, QueryKey queryKey, List nodes) {
        return new FindNodeResponse(getVendor(), getVersion(), getLocalNode(), request.getMessageID(), queryKey, nodes);
    }
    
    public FindValueRequest createFindValueRequest(SocketAddress dst, KUID lookup) {
        if (!lookup.isValueID()) {
            throw new IllegalArgumentException();
        }
        return new FindValueRequest(getVendor(), getVersion(), getLocalNode(), createMessageID(dst), lookup);
    }
    
    public FindValueResponse createFindValueResponse(RequestMessage request, Collection values) {
        return new FindValueResponse(getVendor(), getVersion(), getLocalNode(), request.getMessageID(), values);
    }
    
    public StoreRequest createStoreRequest(SocketAddress dst, int remaining, QueryKey queryKey, Collection values) {
        return new StoreRequest(getVendor(), getVersion(), getLocalNode(), createMessageID(dst), remaining, queryKey, values);
    }
    
    public StoreResponse createStoreResponse(RequestMessage request, int requesting, Collection status) {
        return new StoreResponse(getVendor(), getVersion(), getLocalNode(), request.getMessageID(), requesting, status);
    }
    
    public StatsRequest createStatsRequest(SocketAddress dst, byte[] signature, int request) {
        return new StatsRequest(getVendor(), getVersion(), getLocalNode(), createMessageID(dst), signature, request);
    }

    public StatsResponse createStatsResponse(RequestMessage request, String statistics) {
        return new StatsResponse(getVendor(), getVersion(), getLocalNode(), request.getMessageID(), statistics);
    }
}
