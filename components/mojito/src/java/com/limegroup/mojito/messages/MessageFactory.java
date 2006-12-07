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

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValueEntity;
import com.limegroup.mojito.messages.StatsRequest.StatisticType;
import com.limegroup.mojito.messages.StoreResponse.Status;
import com.limegroup.mojito.routing.Contact;

/**
 * Factory class to construct DHTMessage(s)
 */
public interface MessageFactory {

    /**
     * De-Serializes a DHTMessage from the given ByteBuffer array
     * and returns the message Object
     */
    public DHTMessage createMessage(SocketAddress src, ByteBuffer... data) 
        throws MessageFormatException, IOException;
    
    /**
     * Serializes the given DHTMessage and returns it as a
     * ByteBuffer (NOTE: Make sure the position and limit
     * of the returned ByteBuffer are set properly!)
     */
    public ByteBuffer writeMessage(SocketAddress dst, DHTMessage message) 
            throws IOException;
    
    /**
     * Creates and returns a PingRequest Message
     */
    public PingRequest createPingRequest(Contact contact, MessageID messageId);

    /**
     * Creates and returns a PingResponse Message
     */
    public PingResponse createPingResponse(Contact contact, MessageID messageId, 
            SocketAddress externalAddress, BigInteger estimatedSize);

    /**
     * Creates and returns a FindNodeRequest Message
     */
    public FindNodeRequest createFindNodeRequest(Contact contact, MessageID messageId, 
            KUID lookupId);

    /**
     * Creates and returns a FindNodeResponse Message
     */
    public FindNodeResponse createFindNodeResponse(Contact contact, MessageID messageId, 
            QueryKey queryKey, Collection<? extends Contact> nodes);

    /**
     * Creates and returns a FindValueRequest Message
     */
    public FindValueRequest createFindValueRequest(Contact contact, MessageID messageId, 
            KUID lookupId, Collection<KUID> keys);

    /**
     * Creates and returns a FindValueResponse Message
     */
    public FindValueResponse createFindValueResponse(Contact contact, MessageID messageId, 
            Collection<KUID> keys, Collection<? extends DHTValueEntity> values, float requestLoad);

    /**
     * Creates and returns a StoreRequest Message
     */
    public StoreRequest createStoreRequest(Contact contact, MessageID messageId, 
            QueryKey queryKey, Collection<? extends DHTValueEntity> values);

    /**
     * Creates and returns a StoreResponse Message
     */
    public StoreResponse createStoreResponse(Contact contact, MessageID messageId, 
            Collection<? extends Entry<KUID, Status>> status);

    /**
     * Creates and returns a StatsRequest Message
     */
    public StatsRequest createStatsRequest(Contact contact, MessageID messageId, 
            StatisticType stats);

    /**
     * Creates and returns a StatsResponse Message
     */
    public StatsResponse createStatsResponse(Contact contact, MessageID messageId, String statistics);
}
