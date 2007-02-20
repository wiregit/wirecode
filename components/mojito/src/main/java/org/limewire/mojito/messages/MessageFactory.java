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
 
package org.limewire.mojito.messages;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map.Entry;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.messages.StatsRequest.StatisticType;
import org.limewire.mojito.messages.StoreResponse.Status;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

/**
 * Factory class to construct DHTMessage(s)
 */
public interface MessageFactory {

    /**
     * Creates and returns a MessageID. MessageID implementations
     * that support tagging may use the given SocketAddress to tag
     * and tie the MessageID to the specific Host.
     */
    public MessageID createMessageID(SocketAddress dst);
    
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
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination address to where the request will be send
     */
    public PingRequest createPingRequest(Contact src, SocketAddress dst);

    /**
     * Creates and returns a PingResponse Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination information to where the request will be send
     */
    public PingResponse createPingResponse(Contact src, Contact dst, 
            MessageID messageId, SocketAddress externalAddress, BigInteger estimatedSize);

    /**
     * Creates and returns a FindNodeRequest Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination address to where the request will be send
     */
    public FindNodeRequest createFindNodeRequest(Contact src, SocketAddress dst, KUID lookupId);

    /**
     * Creates and returns a FindNodeResponse Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination information to where the request will be send
     */
    public FindNodeResponse createFindNodeResponse(Contact src, Contact dst, 
            MessageID messageId, Collection<? extends Contact> nodes);

    /**
     * Creates and returns a FindValueRequest Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination address to where the request will be send
     */
    public FindValueRequest createFindValueRequest(Contact src, SocketAddress dst, 
            KUID lookupId, Collection<KUID> keys);

    /**
     * Creates and returns a FindValueResponse Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination information to where the request will be send
     */
    public FindValueResponse createFindValueResponse(Contact src, Contact dst, 
            MessageID messageId, float requestLoad, Collection<? extends DHTValueEntity> entities, Collection<KUID> secondaryKeys);

    /**
     * Creates and returns a StoreRequest Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination address to where the request will be send
     */
    public StoreRequest createStoreRequest(Contact src, SocketAddress dst, 
            SecurityToken securityToken, Collection<? extends DHTValueEntity> values);

    /**
     * Creates and returns a StoreResponse Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination information to where the request will be send
     */
    public StoreResponse createStoreResponse(Contact src, Contact dst, 
            MessageID messageId, Collection<? extends Entry<KUID, Status>> status);

    /**
     * Creates and returns a StatsRequest Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination address to where the request will be send
     */
    public StatsRequest createStatsRequest(Contact src, SocketAddress dst, 
            StatisticType stats);

    /**
     * Creates and returns a StatsResponse Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination information to where the request will be send
     */
    public StatsResponse createStatsResponse(Contact src, Contact dst, 
            MessageID messageId, byte[] statistics);
}
