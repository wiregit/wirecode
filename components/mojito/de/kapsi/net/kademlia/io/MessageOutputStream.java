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

public class MessageOutputStream extends DataOutputStream {
    
    public MessageOutputStream(OutputStream out) {
        super(out);
    }
    
    private void writeNode(Node node) throws IOException {
        writeKUID(node.getNodeID());
        writeSocketAddress(node.getSocketAddress());
    }
    
    /*private void writeTuple(Value tuple) throws IOException {
        writeKUID(tuple.getValueID());
        writeValue(tuple.getValue());
    }*/
    
    private void writeSocketAddress(SocketAddress addr) throws IOException {
        InetSocketAddress iaddr = (InetSocketAddress)addr;
        byte[] address = iaddr.getAddress().getAddress();
        int port = iaddr.getPort();
        
        writeByte(address.length);
        write(address, 0, address.length);
        writeShort(port);
    }
    
    private void writeKUID(KUID key) throws IOException {
        key.write(this);
    }
    
    private void writePublicKey(PublicKey pubKey) throws IOException {
        byte[] encoded = pubKey.getEncoded();
        writeShort(encoded.length);
        write(encoded, 0, encoded.length);
    }
    
    private void writeValue(KeyValue value) throws IOException {
        writeKUID(value.getKey());
        byte[] b = value.getValue();
        writeShort(b.length);
        write(b, 0, b.length);
        
        writePublicKey(value.getPublicKey());
        byte[] signature = value.getSignature();
        writeByte(signature.length);
        write(signature, 0, signature.length);
        
        writeLong(value.getCreationTime());
        writeByte(value.getMode());
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
            writeNode((Node)it.next());
        }
    }
    
    private void writeFindValueRequest(FindValueRequest findValue) throws IOException {
        writeKUID(findValue.getLookupID());
    }
    
    private void writeFindValueResponse(FindValueResponse response) throws IOException {
        writeByte(response.size());
        for(Iterator it = response.iterator(); it.hasNext(); ) {
            writeValue((KeyValue)it.next());
        }
    }
    
    private void writeStoreRequest(StoreRequest store) throws IOException {
        Collection values = store.getValues();
        writeByte(values.size());
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            writeValue((KeyValue)it.next());
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
            throw new IOException("Not implemented");
        } else {
            throw new IOException("Unknown Message: " + msg);
        }
    }
}
