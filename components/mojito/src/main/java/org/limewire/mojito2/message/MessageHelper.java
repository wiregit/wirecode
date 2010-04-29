/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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
 
package org.limewire.mojito2.message;

import java.math.BigInteger;
import java.net.SocketAddress;

import org.limewire.mojito2.Context;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.security.SecurityToken;


/**
 * The {@link MessageHelper} class simplifies the construction of 
 * {@link Message}s.
 */
public class MessageHelper {

    protected final Context context;

    protected final MessageFactory factory;

    public MessageHelper(Context context, MessageFactory factory) {
        this.context = context;
        this.factory = factory;
    }
    
    public MessageFactory getMessageFactory() {
        return factory;
    }
    
    protected Contact getLocalNode() {
        return context.getRouteTable().getLocalNode();
    }
    
    protected BigInteger getEstimatedSize() {
        return context.size();
    }

    public PingRequest createPingRequest(SocketAddress dst) {
        return factory.createPingRequest(getLocalNode(), dst);
    }

    public PingResponse createPingResponse(RequestMessage request, 
            SocketAddress externalAddress) {
        
        if (context.getContactAddress().equals(externalAddress)) {
            throw new IllegalArgumentException("Cannot tell other Node that its external address is the same as yours!");
        }
        
        return factory.createPingResponse(getLocalNode(), request.getContact(), 
                request.getMessageId(), externalAddress, getEstimatedSize());
    }

    public NodeRequest createFindNodeRequest(SocketAddress dst, KUID lookupId) {
        return factory.createNodeRequest(getLocalNode(), dst, lookupId);
    }

    public NodeResponse createFindNodeResponse(RequestMessage request, 
            Contact[] nodes) {
        return factory.createNodeResponse(getLocalNode(), request.getContact(), 
                request.getMessageId(), nodes);
    }

    public ValueRequest createFindValueRequest(SocketAddress dst, KUID lookupId, 
            KUID[] keys, DHTValueType valueType) {
        return factory.createValueRequest(getLocalNode(), dst, lookupId, keys, valueType);
    }

    public ValueResponse createFindValueResponse(RequestMessage request, 
            float requestLoad, DHTValueEntity[] values, KUID[] keys) {
        
        return factory.createValueResponse(getLocalNode(), request.getContact(), 
                request.getMessageId(), requestLoad, values, keys);
    }

    public StoreRequest createStoreRequest(SocketAddress dst, SecurityToken securityToken, 
            DHTValueEntity[] values) {
        
        return factory.createStoreRequest(getLocalNode(), dst, securityToken, values);
    }

    public StoreResponse createStoreResponse(RequestMessage request, 
            StoreStatusCode[] status) {
        
        return factory.createStoreResponse(getLocalNode(), request.getContact(), 
                request.getMessageId(), status);
    }
}
