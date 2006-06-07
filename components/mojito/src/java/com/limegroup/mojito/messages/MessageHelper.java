/*
 * Mojito Distributed Hash Tabe (DHT)
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

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.messages.impl.FindNodeRequestImpl;
import com.limegroup.mojito.messages.impl.FindNodeResponseImpl;
import com.limegroup.mojito.messages.impl.FindValueRequestImpl;
import com.limegroup.mojito.messages.impl.FindValueResponseImpl;
import com.limegroup.mojito.messages.impl.PingRequestImpl;
import com.limegroup.mojito.messages.impl.PingResponseImpl;
import com.limegroup.mojito.messages.impl.StatsRequestImpl;
import com.limegroup.mojito.messages.impl.StatsResponseImpl;
import com.limegroup.mojito.messages.impl.StoreRequestImpl;
import com.limegroup.mojito.messages.impl.StoreResponseImpl;

/**
 * 
 */
public class MessageHelper {

    private Context context;

    private MessageFactory factory;

    public MessageHelper(Context context) {
        this.context = context;
        setMessageFactory(null); // Default
    }

    public void setMessageFactory(MessageFactory factory) {
        if (factory == null) {
            factory = new DefaultMessageFactory();
        }

        this.factory = factory;
    }

    public MessageFactory getMessageFactory() {
        return factory;
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
        if (NetworkUtils.isValidSocketAddress(dst)) {
            return KUID.createRandomMessageID(dst);
        }
        return KUID.MIN_MESSAGE_ID;
    }

    private int getEstimatedSize() {
        return context.size();
    }

    public PingRequest createPingRequest(SocketAddress dst) {
        return factory.createPingRequest(getVendor(), getVersion(), 
                getLocalNode(), createMessageID(dst));
    }

    public PingResponse createPingResponse(RequestMessage request, SocketAddress externalAddress) {
        if (context.getSocketAddress().equals(externalAddress)) {
            throw new IllegalArgumentException("Cannot tell other Node that its external address is the same as yours!");
        }
        
        return factory.createPingResponse(getVendor(), getVersion(), 
                getLocalNode(), request.getMessageID(), externalAddress, getEstimatedSize());
    }

    public FindNodeRequest createFindNodeRequest(SocketAddress dst, KUID lookup) {
        if (!lookup.isNodeID()) {
            throw new IllegalArgumentException();
        }
        
        return factory.createFindNodeRequest(getVendor(), getVersion(), 
                getLocalNode(), createMessageID(dst), lookup);
    }

    public FindNodeResponse createFindNodeResponse(RequestMessage request, QueryKey queryKey, List nodes) {
        return factory.createFindNodeResponse(getVendor(), getVersion(), 
                getLocalNode(), request.getMessageID(), queryKey, nodes);
    }

    public FindValueRequest createFindValueRequest(SocketAddress dst, KUID lookup) {
        if (!lookup.isValueID()) {
            throw new IllegalArgumentException();
        }
        
        return factory.createFindValueRequest(getVendor(), getVersion(), 
                getLocalNode(), createMessageID(dst), lookup);
    }

    public FindValueResponse createFindValueResponse(RequestMessage request, Collection values) {
        return factory.createFindValueResponse(getVendor(), getVersion(), 
                getLocalNode(), request.getMessageID(), values);
    }

    public StoreRequest createStoreRequest(SocketAddress dst, QueryKey queryKey, KeyValue keyValue) {
        return factory.createStoreRequest(getVendor(), getVersion(), 
                getLocalNode(), createMessageID(dst), queryKey, keyValue);
    }

    public StoreResponse createStoreResponse(RequestMessage request, KUID valueId, int status) {
        return factory.createStoreResponse(getVendor(), getVersion(), 
                getLocalNode(), request.getMessageID(), valueId, status);
    }

    public StatsRequest createStatsRequest(SocketAddress dst, int request) {
        return factory.createStatsRequest(getVendor(), getVersion(), 
                getLocalNode(), createMessageID(dst), request);
    }

    public StatsResponse createStatsResponse(RequestMessage request, String statistics) {
        return factory.createStatsResponse(getVendor(), getVersion(), 
                getLocalNode(), request.getMessageID(), statistics);
    }

    /**
     * The default implementation of the MessafeFactory
     */
    private static class DefaultMessageFactory implements MessageFactory {

        public FindNodeRequest createFindNodeRequest(int vendor, int version, 
                ContactNode node, KUID messageId, KUID lookupId) {
            return new FindNodeRequestImpl(vendor, version, node, messageId, lookupId);
        }

        public FindNodeResponse createFindNodeResponse(int vendor, int version, 
                ContactNode node, KUID messageId, QueryKey queryKey, Collection nodes) {
            return new FindNodeResponseImpl(vendor, version, node, messageId, queryKey, nodes);
        }

        public FindValueRequest createFindValueRequest(int vendor, int version, 
                ContactNode node, KUID messageId, KUID lookupId) {
            return new FindValueRequestImpl(vendor, version, node, messageId, lookupId);
        }

        public FindValueResponse createFindValueResponse(int vendor, int version, 
                ContactNode node, KUID messageId, Collection values) {
            return new FindValueResponseImpl(vendor, version, node, messageId, values);
        }

        public PingRequest createPingRequest(int vendor, int version, 
                ContactNode node, KUID messageId) {
            return new PingRequestImpl(vendor, version, node, messageId);
        }

        public PingResponse createPingResponse(int vendor, int version, 
                ContactNode node, KUID messageId, SocketAddress externalAddress, int dhtSize) {
            return new PingResponseImpl(vendor, version, node, messageId, externalAddress, dhtSize);
        }

        public StatsRequest createStatsRequest(int vendor, int version, 
                ContactNode node, KUID messageId, int stats) {
            return new StatsRequestImpl(vendor, version, node, messageId, stats);
        }

        public StatsResponse createStatsResponse(int vendor, int version, 
                ContactNode node, KUID messageId, String stats) {
            return new StatsResponseImpl(vendor, version, node, messageId, stats);
        }

        public StoreRequest createStoreRequest(int vendor, int version, 
                ContactNode node, KUID messageId, QueryKey queryKey, KeyValue keyValue) {
            return new StoreRequestImpl(vendor, version, node, messageId, queryKey, keyValue);
        }

        public StoreResponse createStoreResponse(int vendor, int version, 
                ContactNode node, KUID messageId, KUID valueId, int response) {
            return new StoreResponseImpl(vendor, version, node, messageId, valueId, response);
        }
    }
}
