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
 
package org.limewire.mojito.messages.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Map.Entry;

import org.limewire.io.ByteBufferInputStream;
import org.limewire.io.ByteBufferOutputStream;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.FindNodeRequest;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.messages.MessageFormatException;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.PingRequest;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.messages.StatsRequest;
import org.limewire.mojito.messages.StatsResponse;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.messages.DHTMessage.OpCode;
import org.limewire.mojito.messages.StatsRequest.StatisticType;
import org.limewire.mojito.messages.StoreResponse.Status;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.QueryKey;

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
        
        MessageInputStream in = null;
        
        try {
            in = new MessageInputStream(new ByteBufferInputStream(data));
            
            // --- GNUTELLA HEADER ---
            MessageID messageId = in.readMessageID();
            int func = in.readUnsignedByte();
            if (func != DHTMessage.F_DHT_MESSAGE) {
                throw new MessageFormatException("Unknown function ID: " + func);
            }
            
            int version = in.readUnsignedShort();
            //byte[] length = in.readBytes(4); // Little-Endian!
            in.skip(4);
            
            // --- CONTINUTE WITH MOJITO HEADER ---
            OpCode opcode = in.readOpCode();
            
            switch(opcode) {
                case PING_REQUEST:
                    return new PingRequestImpl(context, src, messageId, version, in);
                case PING_RESPONSE:
                    return new PingResponseImpl(context, src, messageId, version, in);
                case FIND_NODE_REQUEST:
                    return new FindNodeRequestImpl(context, src, messageId, version, in);
                case FIND_NODE_RESPONSE:
                    return new FindNodeResponseImpl(context, src, messageId, version, in);
                case FIND_VALUE_REQUEST:
                    return new FindValueRequestImpl(context, src, messageId, version, in);
                case FIND_VALUE_RESPONSE:
                    return new FindValueResponseImpl(context, src, messageId, version, in);
                case STORE_REQUEST:
                    return new StoreRequestImpl(context, src, messageId, version, in);
                case STORE_RESPONSE:
                    return new StoreResponseImpl(context, src, messageId, version, in);
                case STATS_REQUEST:
                    return new StatsRequestImpl(context, src, messageId, version, in);
                case STATS_RESPONSE:
                    return new StatsResponseImpl(context, src, messageId, version, in);
                default:
                    throw new IOException("Unhandled OpCode " + opcode);
            }
        } catch (IllegalArgumentException err) {
            String msg = (src != null) ? src.toString() : null;
            throw new MessageFormatException(msg, err);
        } catch (IOException err) {
            String msg = (src != null) ? src.toString() : null;
            throw new MessageFormatException(msg, err);
        } finally {
            if (in != null) { 
                try { in.close(); } catch (IOException ignore) {}
            }
        }
    }
    
    public ByteBuffer writeMessage(SocketAddress dst, DHTMessage message) 
            throws IOException {
        ByteBufferOutputStream out = new ByteBufferOutputStream(640, true);
        message.write(out);
        out.close();
        return ((ByteBuffer)out.getBuffer().flip()).order(ByteOrder.BIG_ENDIAN);
    }

    public FindNodeRequest createFindNodeRequest(Contact contact, MessageID messageId, KUID lookupId) {
        return new FindNodeRequestImpl(context, contact, messageId, lookupId);
    }

    public FindNodeResponse createFindNodeResponse(Contact contact, MessageID messageId, 
            QueryKey queryKey, Collection<? extends Contact> nodes) {
        return new FindNodeResponseImpl(context, contact, messageId, queryKey, nodes);
    }

    public FindValueRequest createFindValueRequest(Contact contact, MessageID messageId, 
            KUID lookupId, Collection<KUID> keys) {
        return new FindValueRequestImpl(context, contact, messageId, lookupId, keys);
    }

    public FindValueResponse createFindValueResponse(Contact contact, MessageID messageId, 
            Collection<KUID> keys, Collection<? extends DHTValueEntity> values, float requestLoad) {
        return new FindValueResponseImpl(context, contact, messageId, keys, values, requestLoad);
    }

    public PingRequest createPingRequest(Contact contact, MessageID messageId) {
        return new PingRequestImpl(context, contact, messageId);
    }

    public PingResponse createPingResponse(Contact contact, MessageID messageId, 
            SocketAddress externalAddress, BigInteger estimatedSize) {
        return new PingResponseImpl(context, contact, messageId, externalAddress, estimatedSize);
    }

    public StatsRequest createStatsRequest(Contact contact, MessageID messageId, StatisticType stats) {
        return new StatsRequestImpl(context, contact, messageId, stats);
    }

    public StatsResponse createStatsResponse(Contact contact, MessageID messageId, 
            String statistics) {
        return new StatsResponseImpl(context, contact, messageId, statistics);
    }

    public StoreRequest createStoreRequest(Contact contact, MessageID messageId, 
            QueryKey queryKey, Collection<? extends DHTValueEntity> values) {
        return new StoreRequestImpl(context, contact, messageId, queryKey, values);
    }

    public StoreResponse createStoreResponse(Contact contact, MessageID messageId, 
            Collection<? extends Entry<KUID, Status>> status) {
        return new StoreResponseImpl(context, contact, messageId, status);
    }
}
