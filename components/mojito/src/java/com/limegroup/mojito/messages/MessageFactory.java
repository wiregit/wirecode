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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.messages.StoreResponse.StoreStatus;

/**
 * Factory class to construct DHTMessage(s)
 */
public interface MessageFactory {

    public DHTMessage createMessage(SocketAddress src, ByteBuffer... data) 
        throws MessageFormatException, IOException;
    
    public ByteBuffer writeMessage(SocketAddress dst, DHTMessage message) 
            throws IOException;
    
    public PingRequest createPingRequest(ContactNode contactNode, KUID messageId);

    public PingResponse createPingResponse(ContactNode contactNode, KUID messageId, 
            SocketAddress externalAddress, int estimatedSize);

    public FindNodeRequest createFindNodeRequest(ContactNode contactNode, KUID messageId, 
            KUID lookupId);

    public FindNodeResponse createFindNodeResponse(ContactNode contactNode, KUID messageId, 
            QueryKey queryKey, Collection<ContactNode> nodes);

    public FindValueRequest createFindValueRequest(ContactNode contactNode, KUID messageId, 
            KUID lookupId);

    public FindValueResponse createFindValueResponse(ContactNode contactNode, KUID messageId, 
            Collection<KeyValue> values);

    public StoreRequest createStoreRequest(ContactNode contactNode, KUID messageId, 
            QueryKey queryKey, KeyValue keyValue);

    public StoreResponse createStoreResponse(ContactNode contactNode, KUID messageId, 
            KUID valueId, StoreStatus status);

    public StatsRequest createStatsRequest(ContactNode contactNode, KUID messageId, 
            int stats);

    public StatsResponse createStatsResponse(ContactNode contactNode, KUID messageId, 
            String statistics);
}
