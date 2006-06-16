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

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.messages.impl.DefaultMessageFactory;

/**
 * 
 */
public class MessageHelper {

    protected final Context context;

    private MessageFactory factory;

    public MessageHelper(Context context) {
        this.context = context;
        factory = new DefaultMessageFactory(context);
    }

    public void setMessageFactory(MessageFactory factory) {
        this.factory = factory;
    }

    public MessageFactory getMessageFactory() {
        return factory;
    }

    protected int getVendor() {
        return context.getVendor();
    }

    protected int getVersion() {
        return context.getVersion();
    }

    protected ContactNode getLocalNode() {
        return context.getLocalNode();
    }

    protected KUID createMessageID(SocketAddress dst) {
        if (NetworkUtils.isValidSocketAddress(dst)) {
            return KUID.createRandomMessageID(dst);
        }
        return KUID.MIN_MESSAGE_ID;
    }

    protected int getEstimatedSize() {
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

    public FindNodeRequest createFindNodeRequest(SocketAddress dst, KUID lookupId) {
        if (!lookupId.isNodeID()) {
            throw new IllegalArgumentException();
        }
        
        return factory.createFindNodeRequest(getVendor(), getVersion(), 
                getLocalNode(), createMessageID(dst), lookupId);
    }

    public FindNodeResponse createFindNodeResponse(RequestMessage request, 
            QueryKey queryKey, Collection<ContactNode> nodes) {
        return factory.createFindNodeResponse(getVendor(), getVersion(), 
                getLocalNode(), request.getMessageID(), queryKey, nodes);
    }

    public FindValueRequest createFindValueRequest(SocketAddress dst, KUID lookupId) {
        if (!lookupId.isValueID()) {
            throw new IllegalArgumentException();
        }
        
        return factory.createFindValueRequest(getVendor(), getVersion(), 
                getLocalNode(), createMessageID(dst), lookupId);
    }

    public FindValueResponse createFindValueResponse(RequestMessage request, Collection<KeyValue> values) {
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
}
