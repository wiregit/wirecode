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
import com.limegroup.mojito.Contact;
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
    
    public PingRequest createPingRequest(Contact contact, KUID messageId);

    public PingResponse createPingResponse(Contact contact, KUID messageId, 
            SocketAddress externalAddress, int estimatedSize);

    public FindNodeRequest createFindNodeRequest(Contact contact, KUID messageId, 
            KUID lookupId);

    public FindNodeResponse createFindNodeResponse(Contact contact, KUID messageId, 
            QueryKey queryKey, Collection<? extends Contact> nodes);

    public FindValueRequest createFindValueRequest(Contact contact, KUID messageId, 
            KUID lookupId);

    public FindValueResponse createFindValueResponse(Contact contact, KUID messageId, 
            Collection<KeyValue> values);

    public StoreRequest createStoreRequest(Contact contact, KUID messageId, 
            QueryKey queryKey, KeyValue keyValue);

    public StoreResponse createStoreResponse(Contact contact, KUID messageId, 
            KUID valueId, StoreStatus status);

    public StatsRequest createStatsRequest(Contact contact, KUID messageId, 
            int stats);

    public StatsResponse createStatsResponse(Contact contact, KUID messageId, 
            String statistics);
}
