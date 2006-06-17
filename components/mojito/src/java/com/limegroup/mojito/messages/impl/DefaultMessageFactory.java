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
import com.limegroup.gnutella.util.IntHashMap;
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
import com.limegroup.mojito.messages.DHTMessage.OpCode;

/**
 * The default implementation of the MessageFactory
 */
public class DefaultMessageFactory implements MessageFactory {

    protected final Context context;
    
    // A IntHashMap of int-opcode to enum-opcode
    private static IntHashMap<OpCode> opcodeMap = new IntHashMap<OpCode>();
    
    static {
        for(OpCode opcode : OpCode.values()) {
            opcodeMap.put(opcode.getOpCode(), opcode);
        }
    }
    
    public DefaultMessageFactory(Context context) {
        this.context = context;
    }
    
    /**
     * Returns the enum OpCode for the int version of the opcode. 
     * This method is much faster than OpCode.valueOf(int) which
     * runs in linear time.
     */
    private static OpCode opcode(int opcode) {
        OpCode o = opcodeMap.get(opcode);
        if (o != null) {
            return o;
        }
        
        throw new IllegalArgumentException("Unknown opcode: " + opcode);
    }
    
    public DHTMessage createMessage(SocketAddress src, ByteBuffer data) 
            throws MessageFormatException, IOException {
        
        ByteBuffer guid = (ByteBuffer)data.slice().limit(AbstractMessage.GUID_END);
        ByteBuffer payload = (ByteBuffer)data.position(AbstractMessage.PAYLOAD_START);
        ByteBuffer[] msg = { guid, payload };
        
        OpCode opcode = null;
        try {
            opcode = opcode(msg[0].get() & 0xFF);
        } catch (IllegalArgumentException err) {
            throw new MessageFormatException(err);
        }
        
        try {
            switch(opcode) {
                case PING_REQUEST:
                    return new PingRequestImpl(context, src, msg);
                case PING_RESPONSE:
                    return new PingResponseImpl(context, src, msg);
                case FIND_NODE_REQUEST:
                    return new FindNodeRequestImpl(context, src, msg);
                case FIND_NODE_RESPONSE:
                    return new FindNodeResponseImpl(context, src, msg);
                case FIND_VALUE_REQUEST:
                    return new FindValueRequestImpl(context, src, msg);
                case FIND_VALUE_RESPONSE:
                    return new FindValueResponseImpl(context, src, msg);
                case STORE_REQUEST:
                    return new StoreRequestImpl(context, src, msg);
                case STORE_RESPONSE:
                    return new StoreResponseImpl(context, src, msg);
                case STATS_REQUEST:
                    return new StatsRequestImpl(context, src, msg);
                case STATS_RESPONSE:
                    return new StatsResponseImpl(context, src, msg);
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
            ContactNode node, KUID messageId, String statistics) {
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
