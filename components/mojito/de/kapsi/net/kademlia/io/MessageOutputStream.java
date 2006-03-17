/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Iterator;

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

public class MessageOutputStream extends DataOutputStream {
    
    public MessageOutputStream(OutputStream out) {
        super(out);
    }
	
    private void writeKUID(KUID key) throws IOException {
        if (key != null && !key.isUnknownID()) {
            writeByte(key.getType());
            write(key.getBytes());
        } else {
            writeByte(0);
        }
    }
    
    private void writeKeyValue(KeyValue keyValue) throws IOException {
        writeKUID(keyValue.getKey());
        byte[] b = keyValue.getValue();
        writeShort(b.length);
        write(b, 0, b.length);
        
        writeKUID(keyValue.getNodeID());
        writeSocketAddress(keyValue.getSocketAddress());
        
        writeSignature(keyValue.getSignature());
    }
    
    private void writePublicKey(PublicKey pubKey) throws IOException {
        if (pubKey != null) {
            byte[] encoded = pubKey.getEncoded();
            writeShort(encoded.length);
            write(encoded, 0, encoded.length);
        } else {
            writeShort(0);
        }
    }
    
    private void writeSignature(byte[] signature) throws IOException {
        if (signature != null && signature.length > 0) {
            writeByte(signature.length);
            write(signature, 0, signature.length);
        } else {
            writeByte(0);
        }
    }
    
    private void writeNode(ContactNode node) throws IOException {
        writeKUID(node.getNodeID());
        writeSocketAddress(node.getSocketAddress());
	}
    
    private void writeSocketAddress(SocketAddress addr) throws IOException {
        if (addr instanceof InetSocketAddress) {
            InetSocketAddress iaddr = (InetSocketAddress)addr;
            byte[] address = iaddr.getAddress().getAddress();
            int port = iaddr.getPort();
            
            writeByte(address.length);
            write(address, 0, address.length);
            writeShort(port);
        } else {
            writeByte(0);
        }
    }
    
    private void writePing(PingRequest ping) throws IOException {
        /* WRITE NOTHING */
    }
    
    private void writePong(PingResponse pong) throws IOException {
        writeSocketAddress(pong.getSocketAddress());
    }
    
    private void writeFindNodeRequest(FindNodeRequest findNode) throws IOException {
        writeKUID(findNode.getLookupID());
    }
    
    private void writeFindNodeResponse(FindNodeResponse response) throws IOException {
        writeByte(response.size());
        for(Iterator it = response.iterator(); it.hasNext(); ) {
            writeNode((ContactNode)it.next());
        }
    }
    
    private void writeFindValueRequest(FindValueRequest findValue) throws IOException {
        writeKUID(findValue.getLookupID());
    }
    
    private void writeFindValueResponse(FindValueResponse response) throws IOException {
        writeByte(response.size());
        for(Iterator it = response.iterator(); it.hasNext(); ) {
            writeKeyValue((KeyValue)it.next());
        }
    }
    
    private void writeStoreRequest(StoreRequest request) throws IOException {
        writeShort(request.getRemaingCount());
        
        Collection values = request.getValues();
        writeByte(values.size());
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            writeKeyValue((KeyValue)it.next());
        }
    }
    
    private void writeStoreResponse(StoreResponse response) throws IOException {
        writeShort(response.getRequestCount());
        
        Collection stats = response.getStoreStatus();
        writeByte(stats.size());
        for(Iterator it = stats.iterator(); it.hasNext(); ) {
            StoreResponse.StoreStatus status = (StoreResponse.StoreStatus)it.next();
            writeKUID(status.getKey());
            writeByte(status.getStatus());
        }
    }
    
    public void write(Message msg) throws IOException {
        writeInt(msg.getVendor());
        writeShort(msg.getVersion());
        writeKUID(msg.getNodeID());
        writeKUID(msg.getMessageID());
        
        if (msg instanceof PingRequest) {
            writeByte(Message.PING_REQUEST);
            writePing((PingRequest)msg);
        } else if (msg instanceof PingResponse) {
            writeByte(Message.PING_RESPONSE);
            writePong((PingResponse)msg);
        } else if (msg instanceof FindNodeRequest) {
            writeByte(Message.FIND_NODE_REQUEST);
            writeFindNodeRequest((FindNodeRequest)msg);
        } else if (msg instanceof FindNodeResponse) {
            writeByte(Message.FIND_NODE_RESPONSE);
            writeFindNodeResponse((FindNodeResponse)msg);
        } else if (msg instanceof FindValueRequest) {
            writeByte(Message.FIND_VALUE_REQUEST);
            writeFindValueRequest((FindValueRequest)msg);
        } else if (msg instanceof FindValueResponse) {
            writeByte(Message.FIND_VALUE_RESPONSE);
            writeFindValueResponse((FindValueResponse)msg);
        } else if (msg instanceof StoreRequest) {
            writeByte(Message.STORE_REQUEST);
            writeStoreRequest((StoreRequest)msg);
        } else if (msg instanceof StoreResponse) {
            writeByte(Message.STORE_RESPONSE);
            writeStoreResponse((StoreResponse)msg);
        } else {
            throw new IOException("Unknown Message: " + msg);
        }
    }
}
