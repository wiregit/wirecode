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
 
package com.limegroup.mojito.messages.impl;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.ByteBufferOutputStream;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.messages.MessageFormatException;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;
import com.limegroup.mojito.messages.DHTMessage.OpCode;
import com.limegroup.mojito.messages.StoreResponse.StoreStatus;

/**
 * The default implementation of the MessageFactory
 */
public class DefaultMessageFactory implements MessageFactory {

    protected final Context context;
    
    public DefaultMessageFactory(Context context) {
        this.context = context;
    }
    
    public DHTMessage createMessage(SocketAddress src, ByteBuffer... data) 
            throws MessageFormatException, IOException {
        
        if (data.length == 1) {
            ByteBuffer guid = (ByteBuffer)data[0].slice()
                .position(AbstractMessage.GUID_RANGE[0])
                .limit(AbstractMessage.GUID_RANGE[1]);
            
            ByteBuffer ttlHops = (ByteBuffer)data[0].slice()
                .position(AbstractMessage.TTL_HOPS_RANGE[0])
                .limit(AbstractMessage.TTL_HOPS_RANGE[1]);
            
            ByteBuffer payload = (ByteBuffer)data[0]
              .position(AbstractMessage.PAYLOAD_START);
            
            return createMessage(src, guid, ttlHops, payload);
        }
        
        for(ByteBuffer b : data) {
            b.order(ByteOrder.BIG_ENDIAN);
        }
        
        OpCode opcode = OpCode.valueOf(data[0].get() & 0xFF);
        
        try {
            switch(opcode) {
                case PING_REQUEST:
                    return new PingRequestImpl(context, src, data);
                case PING_RESPONSE:
                    return new PingResponseImpl(context, src, data);
                case FIND_NODE_REQUEST:
                    return new FindNodeRequestImpl(context, src, data);
                case FIND_NODE_RESPONSE:
                    return new FindNodeResponseImpl(context, src, data);
                case FIND_VALUE_REQUEST:
                    return new FindValueRequestImpl(context, src, data);
                case FIND_VALUE_RESPONSE:
                    return new FindValueResponseImpl(context, src, data);
                case STORE_REQUEST:
                    return new StoreRequestImpl(context, src, data);
                case STORE_RESPONSE:
                    return new StoreResponseImpl(context, src, data);
                case STATS_REQUEST:
                    return new StatsRequestImpl(context, src, data);
                case STATS_RESPONSE:
                    return new StatsResponseImpl(context, src, data);
                default:
                    throw new IOException("Unhandled OpCode " + opcode);
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

    public FindNodeRequest createFindNodeRequest(Contact contact, KUID messageId, 
            KUID lookupId) {
        return new FindNodeRequestImpl(context, contact, messageId, lookupId);
    }

    public FindNodeResponse createFindNodeResponse(Contact contact, KUID messageId, 
            QueryKey queryKey, Collection<? extends Contact> nodes) {
        return new FindNodeResponseImpl(context, contact, messageId, queryKey, nodes);
    }

    public FindValueRequest createFindValueRequest(Contact contact, KUID messageId, 
            KUID lookupId) {
        return new FindValueRequestImpl(context, contact, messageId, lookupId);
    }

    public FindValueResponse createFindValueResponse(Contact contact, KUID messageId, 
            Collection<KeyValue> values) {
        return new FindValueResponseImpl(context, contact, messageId, values);
    }

    public PingRequest createPingRequest(Contact contact, KUID messageId) {
        return new PingRequestImpl(context, contact, messageId);
    }

    public PingResponse createPingResponse(Contact contact, KUID messageId, 
            SocketAddress externalAddress, int estimatedSize) {
        return new PingResponseImpl(context, contact, messageId, externalAddress, estimatedSize);
    }

    public StatsRequest createStatsRequest(Contact contact, KUID messageId, 
            int stats) {
        return new StatsRequestImpl(context, contact, messageId, stats);
    }

    public StatsResponse createStatsResponse(Contact contact, KUID messageId, 
            String statistics) {
        return new StatsResponseImpl(context, contact, messageId, statistics);
    }

    public StoreRequest createStoreRequest(Contact contact, KUID messageId, 
            QueryKey queryKey, KeyValue keyValue) {
        return new StoreRequestImpl(context, contact, messageId, queryKey, keyValue);
    }

    public StoreResponse createStoreResponse(Contact contact, KUID messageId, 
            KUID valueId, StoreStatus status) {
        return new StoreResponseImpl(context, contact, messageId, valueId, status);
    }
}
