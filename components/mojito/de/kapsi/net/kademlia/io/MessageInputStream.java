/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.Arrays;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.FindNodeRequest;
import de.kapsi.net.kademlia.messages.request.FindValueRequest;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.messages.response.PingResponse;
import de.kapsi.net.kademlia.messages.response.StoreResponse;
import de.kapsi.net.kademlia.security.CryptoHelper;
import de.kapsi.net.kademlia.security.QueryKey;

public class MessageInputStream extends DataInputStream {
    
    public MessageInputStream(InputStream in) {
        super(in);
    }
    
    private KUID readKUID() throws IOException {
        int type = readUnsignedByte();
        if (type == KUID.UNKNOWN_ID) {
            return null;
        }
        
        byte[] kuid = new byte[KUID.LENGTH/8];
        readFully(kuid);
        switch(type) {
            case KUID.NODE_ID:
                return KUID.createNodeID(kuid);
            case KUID.MESSAGE_ID:
                return KUID.createMessageID(kuid);
            case KUID.VALUE_ID:
                return KUID.createValueID(kuid);
            default:
                throw new IOException("Unknown KUID type: " + type);
        }
    }
    
    private KeyValue readKeyValue() throws IOException {
        
        KUID key = readKUID();
        byte[] value = new byte[readUnsignedShort()];
        readFully(value);
        
        KUID nodeId = readKUID();
        SocketAddress address = readSocketAddress();
        
        byte[] signature = readSignature();
        
        return KeyValue.createRemoteKeyValue(key, value, nodeId, address, signature);
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
        KUID nodeId = readKUID();
        SocketAddress addr = readSocketAddress();
        
        return new ContactNode(nodeId, addr);
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
        return QueryKey.getQueryKey(queryKey);
    }
    
    private PingRequest readPing(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        
        int request = readInt();
        return new PingRequest(vendor, version, nodeId, messageId, request);
    }
    
    private PingResponse readPong(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        SocketAddress addr = readSocketAddress();
        byte[] signature = readSignature();
        return new PingResponse(vendor, version, nodeId, messageId, addr, signature);
    }
    
    private FindNodeRequest readFindNodeRequest(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        KUID lookup = readKUID();
        return new FindNodeRequest(vendor, version, nodeId, messageId, lookup);
    }
    
    private FindNodeResponse readFindNodeResponse(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        
        QueryKey queryKey = readQueryKey();
        int size = readUnsignedByte();
        ContactNode[] nodes = new ContactNode[size];
        for(int i = 0; i < nodes.length; i++) {
            nodes[i] = readContactNode();
        }
        return new FindNodeResponse(vendor, version, nodeId, messageId, queryKey, Arrays.asList(nodes));
    }
    
    private FindValueRequest readFindValueRequest(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        KUID lookup = readKUID();
        return new FindValueRequest(vendor, version, nodeId, messageId, lookup);
    }
    
    private Message readFindValueResponse(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        
        int size = readUnsignedByte();
        KeyValue[] values = new KeyValue[size];
        for(int i = 0; i < values.length; i++) {
            values[i] = readKeyValue();
        }
        return new FindValueResponse(vendor, version, nodeId, messageId, Arrays.asList(values));
    }
    
    private StoreRequest readStoreRequest(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        
        QueryKey queryKey = readQueryKey();
        int remaining = readUnsignedShort();
        
        int size = readUnsignedByte();
        KeyValue[] values = new KeyValue[size];
        for(int i = 0; i < values.length; i++) {
            values[i] = readKeyValue();
        }
        return new StoreRequest(vendor, version, nodeId, messageId, remaining, queryKey, Arrays.asList(values));
    }
    
    private StoreResponse readStoreResponse(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        
        int requesting = readUnsignedShort();
        
        int size = readUnsignedByte();
        StoreResponse.StoreStatus[] stats = new StoreResponse.StoreStatus[size];
        for(int i = 0; i < stats.length; i++) {
            KUID key = readKUID();
            int status = readUnsignedByte();
            stats[i] = new StoreResponse.StoreStatus(key, status);
        }
        
        return new StoreResponse(vendor, version, nodeId, messageId, requesting, Arrays.asList(stats));
    }
    
    public Message readMessage() throws IOException {
        int vendor = readInt();
        int version = readUnsignedShort();
        KUID nodeId = readKUID();
        KUID messageId = readKUID();
        
        int messageType = readUnsignedByte();
        
        switch(messageType) {
            case Message.PING_REQUEST:
                return readPing(vendor, version, nodeId, messageId);
            case Message.PING_RESPONSE:
                return readPong(vendor, version, nodeId, messageId);
            case Message.FIND_NODE_REQUEST:
                return readFindNodeRequest(vendor, version, nodeId, messageId);
            case Message.FIND_NODE_RESPONSE:
                return readFindNodeResponse(vendor, version, nodeId, messageId);
            case Message.FIND_VALUE_REQUEST:
                return readFindValueRequest(vendor, version, nodeId, messageId);
            case Message.FIND_VALUE_RESPONSE:
                return readFindValueResponse(vendor, version, nodeId, messageId);
            case Message.STORE_REQUEST:
                return readStoreRequest(vendor, version, nodeId, messageId);
            case Message.STORE_RESPONSE:
                return readStoreResponse(vendor, version, nodeId, messageId);
            default:
                throw new IOException("Received unknown message type: " + messageType + " from ContactNode: " + nodeId);
        }
    }
}
