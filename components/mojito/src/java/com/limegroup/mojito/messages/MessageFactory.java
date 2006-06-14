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
import com.limegroup.mojito.io.MessageFormatException;

/**
 * Factory class to construct DHTMessage(s)
 */
public interface MessageFactory {

    public DHTMessage createMessage(SocketAddress src, ByteBuffer data) 
            throws MessageFormatException, IOException;
    
    public ByteBuffer writeMessage(SocketAddress dst, DHTMessage message) 
            throws IOException;
    
    public PingRequest createPingRequest(int vendor, int version, 
            ContactNode node, KUID messageId);

    public PingResponse createPingResponse(int vendor, int version, 
            ContactNode node, KUID messageId, SocketAddress externalAddress, int estimatedSize);

    public FindNodeRequest createFindNodeRequest(int vendor, int version, 
            ContactNode node, KUID messageId, KUID lookupId);

    public FindNodeResponse createFindNodeResponse(int vendor, int version, 
            ContactNode node, KUID messageId, QueryKey queryKey, Collection nodes);

    public FindValueRequest createFindValueRequest(int vendor, int version, 
            ContactNode node, KUID messageId, KUID lookupId);

    public FindValueResponse createFindValueResponse(int vendor, int version, 
            ContactNode node, KUID messageId, Collection values);

    public StoreRequest createStoreRequest(int vendor, int version, 
            ContactNode node, KUID messageId, QueryKey queryKey, KeyValue keyValue);

    public StoreResponse createStoreResponse(int vendor, int version, 
            ContactNode node, KUID messageId, KUID valueId, int response);

    public StatsRequest createStatsRequest(int vendor, int version, 
            ContactNode node, KUID messageId, int stats);

    public StatsResponse createStatsResponse(int vendor, int version, 
            ContactNode node, KUID messageId, String stats);
}
