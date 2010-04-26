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

import java.math.BigInteger;
import java.net.SocketAddress;

import org.limewire.io.NetworkUtils;
import org.limewire.mojito.Context2;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

/**
 * The default implementation of the MessageFactory.
 */
public class DefaultMessageFactory implements MessageFactory {

    protected final Context2 context;
    
    public DefaultMessageFactory(Context2 context) {
        this.context = context;
    }
    
    /*public DHTMessage createMessage(SocketAddress src, ByteBuffer... data) 
            throws MessageFormatException, IOException {
        
        MessageInputStream in = null;
        
        try {
            in = new MessageInputStream(new ByteBufferInputStream(data), context.getMACCalculatorRepositoryManager());
            
            // --- GNUTELLA HEADER ---
            MessageID messageId = in.readMessageID();
            int func = in.readUnsignedByte();
            if (func != DHTMessage.F_DHT_MESSAGE) {
                throw new MessageFormatException("Unknown function ID: " + func);
            }
            
            Version msgVersion = in.readVersion();
            //byte[] length = in.readBytes(4); // Little-Endian!
            in.skip(4);
            
            // --- CONTINUTE WITH MOJITO HEADER ---
            OpCode opcode = in.readOpCode();
            
            switch(opcode) {
                case PING_REQUEST:
                    return new PingRequestImpl(context, src, messageId, msgVersion, in);
                case PING_RESPONSE:
                    return new PingResponseImpl(context, src, messageId, msgVersion, in);
                case FIND_NODE_REQUEST:
                    return new FindNodeRequestImpl(context, src, messageId, msgVersion, in);
                case FIND_NODE_RESPONSE:
                    return new FindNodeResponseImpl(context, src, messageId, msgVersion, in);
                case FIND_VALUE_REQUEST:
                    return new FindValueRequestImpl(context, src, messageId, msgVersion, in);
                case FIND_VALUE_RESPONSE:
                    return new FindValueResponseImpl(context, src, messageId, msgVersion, in);
                case STORE_REQUEST:
                    return new StoreRequestImpl(context, src, messageId, msgVersion, in);
                case STORE_RESPONSE:
                    return new StoreResponseImpl(context, src, messageId, msgVersion, in);
                case STATS_REQUEST:
                    return new StatsRequestImpl(context, src, messageId, msgVersion, in);
                case STATS_RESPONSE:
                    return new StatsResponseImpl(context, src, messageId, msgVersion, in);
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
    }*/
    
    public SecurityToken createSecurityToken(Contact dst) {
        return context.getSecurityTokenHelper().createSecurityToken(dst);
    }
    
    public MessageID createMessageID(SocketAddress dst) {
        if (!NetworkUtils.isValidSocketAddress(dst)) {
            throw new IllegalArgumentException(dst + " is an invalid SocketAddress");
        }
        
        return DefaultMessageID.createWithSocketAddress(dst, context.getMACCalculatorRepositoryManager());
    }
    
    /*public ByteBuffer writeMessage(SocketAddress dst, DHTMessage message) 
            throws IOException {
        ByteBufferOutputStream out = new ByteBufferOutputStream(640, true);
        message.write(out);
        out.close();
        return ((ByteBuffer)out.getBuffer().flip()).order(ByteOrder.BIG_ENDIAN);
    }*/

    public NodeRequest createFindNodeRequest(Contact contact, SocketAddress dst, KUID lookupId) {
        return new DefaultNodeRequest(createMessageID(dst), contact, lookupId);
    }

    public NodeResponse createFindNodeResponse(Contact contact, Contact dst, 
            MessageID messageId, Contact[] nodes) {
        return new DefaultNodeResponse(messageId, contact, createSecurityToken(dst), nodes);
    }

    public ValueRequest createFindValueRequest(Contact contact, SocketAddress dst, 
            KUID lookupId, KUID[] keys, DHTValueType valueType) {
        return new DefaultValueRequest(createMessageID(dst), contact, lookupId, keys, valueType);
    }

    public ValueResponse createFindValueResponse(Contact contact, Contact dst, 
            MessageID messageId, float requestLoad, 
            DHTValueEntity[] entities, KUID[] secondaryKeys) {
        return new DefaultValueResponse(messageId, contact, requestLoad, secondaryKeys, entities);
    }

    public PingRequest createPingRequest(Contact contact, SocketAddress dst) {
        return new DefaultPingRequest(createMessageID(dst), contact);
    }

    public PingResponse createPingResponse(Contact contact, Contact dst, 
            MessageID messageId, SocketAddress externalAddress, BigInteger estimatedSize) {
        return new DefaultPingResponse(messageId, contact, externalAddress, estimatedSize);
    }

    public StoreRequest createStoreRequest(Contact contact, SocketAddress dst, 
            SecurityToken securityToken, DHTValueEntity[] values) {
        return new DefaultStoreRequest(createMessageID(dst), contact, securityToken, values);
    }

    public StoreResponse createStoreResponse(Contact contact, Contact dst, 
            MessageID messageId, StoreStatusCode[] status) {
        return new DefaultStoreResponse(messageId, contact, status);
    }
}
