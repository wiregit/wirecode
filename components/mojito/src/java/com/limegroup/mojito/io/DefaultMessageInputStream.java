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

package com.limegroup.mojito.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
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

public class DefaultMessageInputStream extends MessageInputStream {
    
    protected final MessageFactory factory;
    protected final SocketAddress src;
    
    public DefaultMessageInputStream(InputStream in, MessageFactory factory, SocketAddress src) {
        super(in);
        
        this.factory = factory;
        this.src = src;
    }

    private PingRequest readPingRequest(int vendor, int version, 
            ContactNode node, KUID messageId) throws IOException {
        return factory.createPingRequest(vendor, version, node, messageId);
    }
    
    private PingResponse readPingResponse(int vendor, int version, 
            ContactNode node, KUID messageId) throws IOException {
        SocketAddress externalAddress = readSocketAddress();
        int estimatedSize = readInt();
        return factory.createPingResponse(vendor, version, node, messageId, externalAddress, estimatedSize);
    }
    
    private FindNodeRequest readFindNodeRequest(int vendor, int version, 
            ContactNode node, KUID messageId) throws IOException {
        KUID lookup = readNodeID();
        return factory.createFindNodeRequest(vendor, version, node, messageId, lookup);
    }
    
    private FindNodeResponse readFindNodeResponse(int vendor, int version, 
            ContactNode node, KUID messageId) throws IOException {
        QueryKey queryKey = readQueryKey();
        List nodes = readContactNodes();
        return factory.createFindNodeResponse(vendor, version, node, messageId, queryKey, nodes);
    }
    
    private FindValueRequest readFindValueRequest(int vendor, int version, 
            ContactNode node, KUID messageId) throws IOException {
        KUID lookupId = readValueID();
        return factory.createFindValueRequest(vendor, version, node, messageId, lookupId);
    }
    
    private FindValueResponse readFindValueResponse(int vendor, int version, 
            ContactNode node, KUID messageId) throws IOException {
        List values = readKeyValues();
        return factory.createFindValueResponse(vendor, version, node, messageId, values);
    }
    
    private StoreRequest readStoreRequest(int vendor, int version, 
            ContactNode node, KUID messageId) throws IOException {
        QueryKey queryKey = readQueryKey();
        KeyValue keyValue = readKeyValue();
        return factory.createStoreRequest(vendor, version, node, messageId, queryKey, keyValue);
    }
    
    private StoreResponse readStoreResponse(int vendor, int version, 
            ContactNode node, KUID messageId) throws IOException {
        KUID valueId = readValueID();
        int status = readUnsignedByte();
        return factory.createStoreResponse(vendor, version, node, messageId, valueId, status);
    }
    
    private StatsRequest readStatsRequest(int vendor, int version, 
            ContactNode node, KUID messageId) throws IOException {
        int request = readInt();
        return factory.createStatsRequest(vendor, version, node, messageId, request);
    }
    
    private StatsResponse readStatsResponse(int vendor, int version, ContactNode node,
            KUID messageId) throws IOException{
        String stats = readUTF();
        return factory.createStatsResponse(vendor, version, node, messageId, stats);
    }
    
    public DHTMessage readMessage() throws IOException {
        
        int opcode = readUnsignedByte();
        int vendor = readInt();
        int version = readUnsignedShort();
        int flags = readUnsignedByte();
        KUID nodeId = readNodeID();
        int instanceId = readUnsignedByte();
        KUID messageId = readMessageID();
        
        ContactNode node = new ContactNode(nodeId, src, instanceId, flags);

        switch(opcode) {
            case DHTMessage.PING_REQUEST:
                return readPingRequest(vendor, version, node, messageId);
            case DHTMessage.PING_RESPONSE:
                return readPingResponse(vendor, version, node, messageId);
            case DHTMessage.FIND_NODE_REQUEST:
                return readFindNodeRequest(vendor, version, node, messageId);
            case DHTMessage.FIND_NODE_RESPONSE:
                return readFindNodeResponse(vendor, version, node, messageId);
            case DHTMessage.FIND_VALUE_REQUEST:
                return readFindValueRequest(vendor, version, node, messageId);
            case DHTMessage.FIND_VALUE_RESPONSE:
                return readFindValueResponse(vendor, version, node, messageId);
            case DHTMessage.STORE_REQUEST:
                return readStoreRequest(vendor, version, node, messageId);
            case DHTMessage.STORE_RESPONSE:
                return readStoreResponse(vendor, version, node, messageId);
            case DHTMessage.STATS_REQUEST:
                return readStatsRequest(vendor, version, node, messageId);
            case DHTMessage.STATS_RESPONSE:
                return readStatsResponse(vendor, version, node, messageId);
            default:
                throw new IOException("Received unknown message type: " + opcode + " from ContactNode: " + nodeId);
        }
    }
}
