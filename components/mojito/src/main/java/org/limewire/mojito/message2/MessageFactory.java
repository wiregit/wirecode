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
 
package org.limewire.mojito.message2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.security.SecurityTokenHelper2;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.security.SecurityToken.TokenData;

/**
 * Defines the interface for a factory to construct DHT messages.
 */
public interface MessageFactory {

    /**
     * 
     */
    public MACCalculatorRepositoryManager getMACCalculatorRepositoryManager();
    
    /**
     * 
     */
    public SecurityTokenHelper2 getSecurityTokenHelper();
    
    /**
     * 
     */
    public SecurityToken createSecurityToken(Contact dst);
    
    /**
     * 
     */
    public TokenData createTokenData(Contact src);
    
    /**
     */
    public byte[] serialize(Message message) throws IOException;
    
    /**
     * 
     */
    public Message deserialize(SocketAddress src, 
            byte[] message, int offset, int length) throws IOException;
    
    
    /**
     * Creates and returns a MessageID. MessageID implementations
     * that support tagging may use the given SocketAddress to tag
     * and tie the MessageID to the specific Host.
     * 
     * @param dst the <code>MessageID</code> is tagged with this 
     * <code>SocketAddress</code>
     */
    public MessageID createMessageID(SocketAddress dst);
    
    /**
     * Creates and returns a <code>PingRequest</code> Message.
     * 
     * @param src the contact information of the issuing Node
     * @param dst the destination address to where the request will be send
     */
    public PingRequest createPingRequest(Contact src, SocketAddress dst);

    /**
     * Creates and returns a <code>PingResponse</code> Message.
     * 
     * @param src the contact information of the issuing Node
     * @param dst the destination information to where the request will be send
     * @param messageId the Message ID
     * @param externalAddress IP address to contact the issuing Node
     * @param estimatedSize upper bound size estimate of the ping response message
     */
    public PingResponse createPingResponse(Contact src, Contact dst, 
            MessageID messageId, SocketAddress externalAddress, BigInteger estimatedSize);

    /**
     * Creates and returns a <code>FindNodeRequest</code> Message.
     * 
     * @param src the contact information of the issuing Node
     * @param dst the destination address to where the request will be send
     * @param lookupID node id to use for the lookup
     */
    public NodeRequest createNodeRequest(Contact src, SocketAddress dst, KUID lookupId);

    /**
     * Creates and returns a <code>FindNodeResponse</code> Message.
     * 
     * @param src the contact information of the issuing Node
     * @param dst the destination information to where the request will be send
     * @param messageId the Message ID
     * @param nodes nodes in the issuing Node's bucket that are closest
     * to the requested target id
     */
    public NodeResponse createNodeResponse(Contact src, Contact dst, 
            MessageID messageId, Contact[] nodes);

    /**
     * Creates and returns a <code>FindValueRequest</code> Message.
     * 
     * @param src the contact information of the issuing Node
     * @param dst the destination address to where the request will be send
     * @param primaryKey the primary key we're looking for
     * @param secondaryKeys a Collection of secondary Keys we're looking for (can be empty)
     * @param valueType the type of value we're looking for
     */
    public ValueRequest createValueRequest(Contact src, SocketAddress dst, 
            KUID primaryKey, KUID[] secondaryKeys, DHTValueType valueType);

    /**
     * Creates and returns a <code>FindValueResponse</code> Message.
     * 
     * @param src the contact information of the issuing Node
     * @param dst the destination information to where the request will be send
     * @param messageId the Message ID
     * @param requestLoad Exponential Moving Average smoothing factor
     * (http://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average)
     * @param entities DHT values in the issuing Node's bucket that are the closest 
     * to the requested message id
     * @param secondaryKeys secondary keys of the values
     * 
     */
    public ValueResponse createValueResponse(Contact src, Contact dst, 
            MessageID messageId, float requestLoad, DHTValueEntity[] entities, KUID[] secondaryKeys);

    /**
     * Creates and returns a StoreRequest Message
     * 
     * @param src the contact information of the issuing Node
     * @param dst the destination address to where the request will be send
     * @param securityToken token to authenticate a host
     * @param values collection of <code>DHTValueEntity</code>(ies) to do
     * a store request
     */
    public StoreRequest createStoreRequest(Contact src, SocketAddress dst, 
            SecurityToken securityToken, DHTValueEntity[] values);

    /**
     * Creates and returns a StoreResponse Message
     * 
     * @param src the contact information of the issuing Node
     * @param dst the destination information to where the request will be send
     * @param messageId the Message ID
     * @param status collection of status whether or not it was stored at the 
     * remote Node
     */
    public StoreResponse createStoreResponse(Contact src, Contact dst, 
            MessageID messageId, StoreStatusCode[] status);
}
