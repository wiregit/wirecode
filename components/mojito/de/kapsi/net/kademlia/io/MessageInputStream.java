/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Node;
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

public class MessageInputStream extends DataInputStream {
    
    public MessageInputStream(InputStream in) {
        super(in);
    }
    
    private byte[] readKUIDBytes() throws IOException {
        byte[] kuid = new byte[KUID.LENGTH/8];
        readFully(kuid);
        return kuid;
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
    
    private KeyValue readValue() throws IOException {
        
        KUID key = readValueID();
        byte[] value = new byte[readUnsignedShort()];
        readFully(value);
        
        PublicKey pubKey = readPublicKey();
        byte[] signature = new byte[readUnsignedByte()];
        readFully(signature);
        
        long creationTime = readLong();
        int mode = readUnsignedByte();
        
        try {
            return new KeyValue(key, value, pubKey, signature, creationTime, mode);
        } catch (SignatureException err) {
            throw new IOException(err.getMessage());
        } catch (InvalidKeyException err) {
            throw new IOException(err.getMessage());
        }
    }
    
    private PublicKey readPublicKey() throws IOException {
        byte[] encodedKey = new byte[readUnsignedShort()];
        readFully(encodedKey);
        return CryptoHelper.createPublicKey(encodedKey);
    }
    
    private Node readNode() throws IOException {
        KUID nodeId = readNodeID();
        SocketAddress addr = readSocketAddress();
        
        return new Node(nodeId, addr);
    }
    
    private InetSocketAddress readSocketAddress() throws IOException {
        byte[] address = new byte[readUnsignedByte()];
        readFully(address);
        
        int port = readUnsignedShort();
        return new InetSocketAddress(InetAddress.getByAddress(address), port);
    }
    
    private PingRequest readPing(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        return new PingRequest(vendor, version, nodeId, messageId);
    }
    
    private PingResponse readPong(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        SocketAddress addr = readSocketAddress();
        return new PingResponse(vendor, version, nodeId, messageId, addr);
    }
    
    private FindNodeRequest readFindNodeRequest(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        KUID lookup = readNodeID();
        return new FindNodeRequest(vendor, version, nodeId, messageId, lookup);
    }
    
    private FindNodeResponse readFindNodeResponse(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        final int size = readUnsignedByte();
        Node[] nodes = new Node[size];
        for(int i = 0; i < nodes.length; i++) {
            nodes[i] = readNode();
        }
        return new FindNodeResponse(vendor, version, nodeId, messageId, Arrays.asList(nodes));
    }
    
    private FindValueRequest readFindValueRequest(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        KUID lookup = readValueID();
        return new FindValueRequest(vendor, version, nodeId, messageId, lookup);
    }
    
    private Message readFindValueResponse(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        int size = readUnsignedByte();
        KeyValue[] values = new KeyValue[size];
        for(int i = 0; i < values.length; i++) {
            values[i] = readValue();
        }
        return new FindValueResponse(vendor, version, nodeId, messageId, Arrays.asList(values));
    }
    
    private StoreRequest readStoreRequest(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        int size = readUnsignedByte();
        KeyValue[] values = new KeyValue[size];
        for(int i = 0; i < values.length; i++) {
            values[i] = readValue();
        }
        return new StoreRequest(vendor, version, nodeId, messageId, Arrays.asList(values));
    }
    
    private StoreResponse readStoreResponse(int vendor, int version, 
            KUID nodeId, KUID messageId) throws IOException {
        
        StoreResponse.Status[] stats = new StoreResponse.Status[read()];
        for(int i = 0; i < stats.length; i++) {
            KUID key = readValueID();
            int status = read();
            stats[i] = new StoreResponse.Status(key, status);
        }
        
        return new StoreResponse(vendor, version, nodeId, messageId, Arrays.asList(stats));
    }
    
    public Message readMessage() throws IOException {
        int vendor = readInt();
        int version = readUnsignedShort();
        KUID nodeId = readNodeID();
        KUID messageId = readMessageID();
        
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
                throw new IOException("Received unknown message type: " + messageType + " from Node: " + nodeId);
        }
    }
}
