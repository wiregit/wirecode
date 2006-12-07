/*
 * Mojito Distributed Hash Table (Mojito DHT)
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
 
package com.limegroup.mojito.messages;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValueEntity;
import com.limegroup.mojito.messages.StatsRequest.StatisticType;
import com.limegroup.mojito.messages.StoreResponse.Status;
import com.limegroup.mojito.messages.impl.DefaultMessageFactory;
import com.limegroup.mojito.routing.Contact;

/**
 * The MessageHelper class simplifies the construction of DHTMessages.
 * Except for some rare cases you want to use the MessageHelper instead of
 * the MessageFactory
 */
public class MessageHelper {

    protected final Context context;

    private MessageFactory factory;

    public MessageHelper(Context context) {
        this.context = context;
        factory = new DefaultMessageFactory(context);
    }

    public void setMessageFactory(MessageFactory factory) {
        if (factory == null) {
            factory = new DefaultMessageFactory(context);
        }
        this.factory = factory;
    }

    public MessageFactory getMessageFactory() {
        return factory;
    }
    
    protected Contact getLocalNode() {
        return context.getLocalNode();
    }

    protected MessageID createMessageID(SocketAddress dst) {
        if (!NetworkUtils.isValidSocketAddress(dst)) {
            throw new IllegalArgumentException(dst + " is an invalid SocketAddress");
        }
        
        return MessageID.createWithSocketAddress(dst);
    }

    protected BigInteger getEstimatedSize() {
        return context.size();
    }

    public PingRequest createPingRequest(SocketAddress dst) {
        return factory.createPingRequest(getLocalNode(), createMessageID(dst));
    }

    public PingResponse createPingResponse(RequestMessage request, SocketAddress externalAddress) {
        if (context.getContactAddress().equals(externalAddress)) {
            throw new IllegalArgumentException("Cannot tell other Node that its external address is the same as yours!");
        }
        
        return factory.createPingResponse(getLocalNode(), request.getMessageID(), 
                externalAddress, getEstimatedSize());
    }

    public FindNodeRequest createFindNodeRequest(SocketAddress dst, KUID lookupId) {
        return factory.createFindNodeRequest(getLocalNode(), 
                createMessageID(dst), lookupId);
    }

    public FindNodeResponse createFindNodeResponse(RequestMessage request, 
            QueryKey queryKey, Collection<? extends Contact> nodes) {
        return factory.createFindNodeResponse(getLocalNode(), request.getMessageID(), 
                queryKey, nodes);
    }

    public FindValueRequest createFindValueRequest(SocketAddress dst, KUID lookupId, 
            Collection<KUID> keys) {
        
        return factory.createFindValueRequest(getLocalNode(), 
                createMessageID(dst), lookupId, keys);
    }

    public FindValueResponse createFindValueResponse(RequestMessage request, 
            Collection<KUID> keys, Collection<? extends DHTValueEntity> values, float requestLoad) {
        return factory.createFindValueResponse(getLocalNode(), request.getMessageID(), keys, 
                values, requestLoad);
    }

    public StoreRequest createStoreRequest(SocketAddress dst, QueryKey queryKey, 
            Collection<? extends DHTValueEntity> values) {
        return factory.createStoreRequest(getLocalNode(), createMessageID(dst), queryKey, values);
    }

    public StoreResponse createStoreResponse(RequestMessage request, 
            Collection<? extends Entry<KUID, Status>> status) {
        return factory.createStoreResponse(getLocalNode(), request.getMessageID(), status);
    }

    public StatsRequest createStatsRequest(SocketAddress dst, StatisticType request) {
        return factory.createStatsRequest(getLocalNode(), createMessageID(dst), request);
    }

    public StatsResponse createStatsResponse(RequestMessage request, String statistics) {
        return factory.createStatsResponse(getLocalNode(), request.getMessageID(), statistics);
    }
}
