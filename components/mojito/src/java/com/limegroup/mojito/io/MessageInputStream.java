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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
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
import com.limegroup.mojito.security.CryptoHelper;

/**
 * The MessageInputStream reads (parses) a DHTMessage
 * from a given InputStream
 */
public class MessageInputStream extends DataInputStream {
    
    private MessageFactory factory;
    private SocketAddress src;
    
    public MessageInputStream(InputStream in, 
            MessageFactory factory, SocketAddress src) {
        super(in);
        
        this.factory = factory;
        this.src = src;
    }
    
    private byte[] readKUIDBytes() throws IOException {
        byte[] id = new byte[KUID.LENGTH/8];
        readFully(id);
        return id;
    }
    
    private KUID readNodeID() throws IOException {
        return KUID.createNodeID(readKUIDBytes());
    }
    
    private KUID readMessageID() throws IOException {
        return KUID.createMessageID(readKUIDBytes());
    }
    
    private KUID readValueID() throws IOException {
        return KUID.createValueID(readKUIDBytes());
    }
    
    private KeyValue readKeyValue() throws IOException {
        KUID key = readValueID();
        byte[] value = new byte[readUnsignedShort()];
        readFully(value);
        
        KUID nodeId = readNodeID();
        SocketAddress address = readSocketAddress();
        
        PublicKey pubKey = readPublicKey();
        byte[] signature = readSignature();
        
        return KeyValue.createRemoteKeyValue(key, value, nodeId, address, pubKey, signature);
    }
    
    private List readKeyValues() throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.EMPTY_LIST;
        }
        
        KeyValue[] values = new KeyValue[size];
        for(int i = 0; i < values.length; i++) {
            values[i] = readKeyValue();
        }
        return Arrays.asList(values);
    }
    
    private PublicKey readPublicKey() throws IOException {
        int length = readUnsignedShort();
        if (length == 0) {
            return null;
        }
        
        byte[] encodedKey = new byte[length];
        readFully(encodedKey);
        return CryptoHelper.createPublicKey(encodedKey);
    }
    
	private byte[] readSignature() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] signature = new byte[length];
        readFully(signature, 0, signature.length);
        return signature;
    }
	
    private ContactNode readContactNode() throws IOException {
        KUID nodeId = readNodeID();
        SocketAddress addr = readSocketAddress();
        return new ContactNode(nodeId, addr);
    }
    
    private List readContactNodes() throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.EMPTY_LIST;
        }
        
        ContactNode[] nodes = new ContactNode[size];
        for(int i = 0; i < nodes.length; i++) {
            nodes[i] = readContactNode();
        }
        return Arrays.asList(nodes);
    }

    private InetSocketAddress readSocketAddress() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] address = new byte[length];
        readFully(address);
        
        int port = readUnsignedShort();
        return new InetSocketAddress(InetAddress.getByAddress(address), port);
    }
    
    private QueryKey readQueryKey() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] queryKey = new byte[length];
        readFully(queryKey, 0, queryKey.length);
        return QueryKey.getQueryKey(queryKey, true);
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
