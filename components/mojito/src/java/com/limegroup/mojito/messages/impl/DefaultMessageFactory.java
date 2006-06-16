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

package com.limegroup.mojito.messages.impl;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.ByteBufferOutputStream;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.io.MessageFormatException;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;

/**
 * The default implementation of the MessageFactory
 */
public class DefaultMessageFactory implements MessageFactory {

    /*
     * The Gnutella Message Header we have to skip.
     * See AbstractDHTMessage for more info!
     */
    private static final int GNUTELLA_MESSAGE_HEADER = 23;
    
    protected final Context context;
    
    public DefaultMessageFactory(Context context) {
        this.context = context;
    }
    
    public DHTMessage createMessage(SocketAddress src, ByteBuffer data) 
            throws MessageFormatException, IOException {
        
        data.position(GNUTELLA_MESSAGE_HEADER);
        data.mark();
        
        int opcode = data.get() & 0xFF;
        
        try {
            switch(opcode) {
                case DHTMessage.PING_REQUEST:
                    return new PingRequestImpl(context, src, data);
                case DHTMessage.PING_RESPONSE:
                    return new PingResponseImpl(context, src, data);
                case DHTMessage.FIND_NODE_REQUEST:
                    return new FindNodeRequestImpl(context, src, data);
                case DHTMessage.FIND_NODE_RESPONSE:
                    return new FindNodeResponseImpl(context, src, data);
                case DHTMessage.FIND_VALUE_REQUEST:
                    return new FindValueRequestImpl(context, src, data);
                case DHTMessage.FIND_VALUE_RESPONSE:
                    return new FindValueResponseImpl(context, src, data);
                case DHTMessage.STORE_REQUEST:
                    return new StoreRequestImpl(context, src, data);
                case DHTMessage.STORE_RESPONSE:
                    return new StoreResponseImpl(context, src, data);
                case DHTMessage.STATS_REQUEST:
                    return new StatsRequestImpl(context, src, data);
                case DHTMessage.STATS_RESPONSE:
                    return new StatsResponseImpl(context, src, data);
                default:
                    throw new IOException("Received unknown message type: " + opcode + " from " + src);
            }
        } catch (IOException err) {
            throw new MessageFormatException(err);
        }
    }
    
    public ByteBuffer writeMessage(SocketAddress dst, DHTMessage message) 
            throws IOException {
        ByteBufferOutputStream out = new ByteBufferOutputStream(640, true);
        message.write(out);
        out.close();
        return ((ByteBuffer)out.buffer().flip()).order(ByteOrder.BIG_ENDIAN);
    }

    public FindNodeRequest createFindNodeRequest(int vendor, int version, 
            ContactNode node, KUID messageId, KUID lookupId) {
        return new FindNodeRequestImpl(context, vendor, version, node, messageId, lookupId);
    }

    public FindNodeResponse createFindNodeResponse(int vendor, int version, 
            ContactNode node, KUID messageId, QueryKey queryKey, Collection<ContactNode> nodes) {
        return new FindNodeResponseImpl(context, vendor, version, node, messageId, queryKey, nodes);
    }

    public FindValueRequest createFindValueRequest(int vendor, int version, 
            ContactNode node, KUID messageId, KUID lookupId) {
        return new FindValueRequestImpl(context, vendor, version, node, messageId, lookupId);
    }

    public FindValueResponse createFindValueResponse(int vendor, int version, 
            ContactNode node, KUID messageId, Collection<KeyValue> values) {
        return new FindValueResponseImpl(context, vendor, version, node, messageId, values);
    }

    public PingRequest createPingRequest(int vendor, int version, 
            ContactNode node, KUID messageId) {
        return new PingRequestImpl(context, vendor, version, node, messageId);
    }

    public PingResponse createPingResponse(int vendor, int version, 
            ContactNode node, KUID messageId, SocketAddress externalAddress, int estimatedSize) {
        return new PingResponseImpl(context, vendor, version, node, messageId, externalAddress, estimatedSize);
    }

    public StatsRequest createStatsRequest(int vendor, int version, 
            ContactNode node, KUID messageId, int stats) {
        return new StatsRequestImpl(context, vendor, version, node, messageId, stats);
    }

    public StatsResponse createStatsResponse(int vendor, int version, 
            ContactNode node, KUID messageId, byte[] statistics) {
        return new StatsResponseImpl(context, vendor, version, node, messageId, statistics);
    }

    public StoreRequest createStoreRequest(int vendor, int version, 
            ContactNode node, KUID messageId, QueryKey queryKey, KeyValue keyValue) {
        return new StoreRequestImpl(context, vendor, version, node, messageId, queryKey, keyValue);
    }

    public StoreResponse createStoreResponse(int vendor, int version, 
            ContactNode node, KUID messageId, KUID valueId, int response) {
        return new StoreResponseImpl(context, vendor, version, node, messageId, valueId, response);
    }
}
