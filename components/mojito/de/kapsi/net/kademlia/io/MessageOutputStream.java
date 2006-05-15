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
import de.kapsi.net.kademlia.messages.DHTMessage;
import de.kapsi.net.kademlia.messages.request.FindNodeRequest;
import de.kapsi.net.kademlia.messages.request.FindValueRequest;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.request.StatsRequest;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.messages.response.PingResponse;
import de.kapsi.net.kademlia.messages.response.StatsResponse;
import de.kapsi.net.kademlia.messages.response.StoreResponse;
import de.kapsi.net.kademlia.security.QueryKey;

public class MessageOutputStream extends DataOutputStream {
    
    public MessageOutputStream(OutputStream out) {
        super(out);
    }
	
    private void writeKUID(KUID key) throws IOException {
        if (key != null) {
            writeByte(key.getType());
            write(key.getBytes());
        } else {
            writeByte(0);
        }
    }
    
    private void writeKeyValue(KeyValue keyValue) throws IOException {
        writeKUID((KUID)keyValue.getKey());
        byte[] b = (byte[])keyValue.getValue();
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
    
    private void writeContactNode(ContactNode node) throws IOException {
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
    
    private void writeQueryKey(QueryKey queryKey) throws IOException {
        if (queryKey != null) {
            byte[] qk = queryKey.getBytes();
            writeByte(qk.length);
            write(qk, 0, qk.length);
        } else {
            writeByte(0);
        }
    }
    
    private void writePing(PingRequest ping) throws IOException {
        /* NOTHING TO WRITE */
        //writeInt(0);
    }
    
    private void writePong(PingResponse pong) throws IOException {
        writeSocketAddress(pong.getExternalSocketAddress());
        writeInt(pong.getEstimatedSize());
        writeSignature(pong.getSignature());
    }
    
    private void writeFindNodeRequest(FindNodeRequest findNode) throws IOException {
        writeKUID(findNode.getLookupID());
    }
    
    private void writeFindNodeResponse(FindNodeResponse response) throws IOException {
        writeQueryKey(response.getQueryKey());
        
        Collection values = response.getValues();
        writeByte(values.size());
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            writeContactNode((ContactNode)it.next());
        }
    }
    
    private void writeFindValueRequest(FindValueRequest findValue) throws IOException {
        writeKUID(findValue.getLookupID());
    }
    
    private void writeFindValueResponse(FindValueResponse response) throws IOException {
        
        Collection values = response.getValues();
        writeByte(values.size());
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            writeKeyValue((KeyValue)it.next());
        }
    }
    
    private void writeStoreRequest(StoreRequest request) throws IOException {
        
        writeQueryKey(request.getQueryKey());
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
    
    private void writeStatsRequest(StatsRequest request) throws IOException {
        writeSignature(request.getSignature());
        writeInt(request.getRequest());
    }
    
    private void writeStatsResponse(StatsResponse response) throws IOException {
        writeUTF(response.getStatistics());
    }
    
    public void write(DHTMessage msg) throws IOException {
        writeInt(msg.getVendor());
        writeShort(msg.getVersion());
        writeByte(msg.getSource().getFlags());
        writeKUID(msg.getSource().getNodeID());
        writeKUID(msg.getMessageID());
        writeByte(msg.getSource().getInstanceID());
        
        if (msg instanceof PingRequest) {
            writeByte(DHTMessage.PING_REQUEST);
            writePing((PingRequest)msg);
        } else if (msg instanceof PingResponse) {
            writeByte(DHTMessage.PING_RESPONSE);
            writePong((PingResponse)msg);
        } else if (msg instanceof FindNodeRequest) {
            writeByte(DHTMessage.FIND_NODE_REQUEST);
            writeFindNodeRequest((FindNodeRequest)msg);
        } else if (msg instanceof FindNodeResponse) {
            writeByte(DHTMessage.FIND_NODE_RESPONSE);
            writeFindNodeResponse((FindNodeResponse)msg);
        } else if (msg instanceof FindValueRequest) {
            writeByte(DHTMessage.FIND_VALUE_REQUEST);
            writeFindValueRequest((FindValueRequest)msg);
        } else if (msg instanceof FindValueResponse) {
            writeByte(DHTMessage.FIND_VALUE_RESPONSE);
            writeFindValueResponse((FindValueResponse)msg);
        } else if (msg instanceof StoreRequest) {
            writeByte(DHTMessage.STORE_REQUEST);
            writeStoreRequest((StoreRequest)msg);
        } else if (msg instanceof StoreResponse) {
            writeByte(DHTMessage.STORE_RESPONSE);
            writeStoreResponse((StoreResponse)msg);
        } else if (msg instanceof StatsRequest) {
            writeByte(DHTMessage.STATS_REQUEST);
            writeStatsRequest((StatsRequest)msg);
        } else if (msg instanceof StatsResponse) {
            writeByte(DHTMessage.STATS_RESPONSE);
            writeStatsResponse((StatsResponse)msg);
        } else {
            throw new IOException("Unknown Message: " + msg);
        }
    }
}
