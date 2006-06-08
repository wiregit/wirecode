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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Iterator;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;

/**
 * The MessageOutputStream class writes a DHTMessage (serializes)
 * to a given OutputStream.
 */
public class MessageOutputStream extends DataOutputStream {
    
    public MessageOutputStream(OutputStream out) {
        super(out);
    }
	
    private void writeKUID(KUID key) throws IOException {
        if (key == null) {
            throw new NullPointerException("KUID cannot be null");
        }
        
        write(key.getBytes());
    }
    
    private void writeKeyValue(KeyValue keyValue) throws IOException {
        writeKUID((KUID)keyValue.getKey());
        byte[] b = (byte[])keyValue.getValue();
        writeShort(b.length);
        write(b, 0, b.length);
        
        writeKUID(keyValue.getNodeID());
        writeSocketAddress(keyValue.getSocketAddress());
        
        writePublicKey(keyValue.getPublicKey());
        writeSignature(keyValue.getSignature());
    }
    
    private void writeKeyValues(Collection values) throws IOException {
        writeByte(values.size());
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            writeKeyValue((KeyValue)it.next());
        }
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
    
    private void writeContactNodes(Collection nodes) throws IOException {
        writeByte(nodes.size());
        for(Iterator it = nodes.iterator(); it.hasNext(); ) {
            writeContactNode((ContactNode)it.next());
        }
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
    
    private void writePingRequest(PingRequest ping) throws IOException {
        /* NOTHING TO WRITE */
    }
    
    private void writePingResponse(PingResponse pong) throws IOException {
        writeSocketAddress(pong.getExternalAddress());
        writeInt(pong.getEstimatedSize());
    }
    
    private void writeFindNodeRequest(FindNodeRequest findNode) throws IOException {
        writeKUID(findNode.getLookupID());
    }
    
    private void writeFindNodeResponse(FindNodeResponse response) throws IOException {
        writeQueryKey(response.getQueryKey());
        writeContactNodes(response.getNodes());
    }
    
    private void writeFindValueRequest(FindValueRequest findValue) throws IOException {
        writeKUID(findValue.getLookupID());
    }
    
    private void writeFindValueResponse(FindValueResponse response) throws IOException {
        writeKeyValues(response.getValues());
    }
    
    private void writeStoreRequest(StoreRequest request) throws IOException {
        writeQueryKey(request.getQueryKey());
        writeKeyValue(request.getKeyValue());
    }
    
    private void writeStoreResponse(StoreResponse response) throws IOException {
        writeKUID(response.getValueID());
        writeByte(response.getStatus());
    }
    
    private void writeStatsRequest(StatsRequest request) throws IOException {
        writeSignature(request.getSignature());
        writeInt(request.getRequest());
    }
    
    private void writeStatsResponse(StatsResponse response) throws IOException {
        writeUTF(response.getStatistics());
    }

    private void writeHeader(DHTMessage msg) throws IOException {
        writeByte(msg.getOpCode());
        writeInt(msg.getVendor());
        writeShort(msg.getVersion());
        writeByte(msg.getContactNode().getFlags());
        writeKUID(msg.getContactNode().getNodeID());
        writeByte(msg.getContactNode().getInstanceID());
        writeKUID(msg.getMessageID());
    }
    
    public void write(DHTMessage msg) throws IOException {
        writeHeader(msg);
        
        switch(msg.getOpCode()) {
            case DHTMessage.PING_REQUEST:
                writePingRequest((PingRequest)msg);
                break;
            case DHTMessage.PING_RESPONSE:
                writePingResponse((PingResponse)msg);
                break;
            case DHTMessage.STORE_REQUEST:
                writeStoreRequest((StoreRequest)msg);
                break;
            case DHTMessage.STORE_RESPONSE:
                writeStoreResponse((StoreResponse)msg);
                break;
            case DHTMessage.FIND_NODE_REQUEST:
                writeFindNodeRequest((FindNodeRequest)msg);
                break;
            case DHTMessage.FIND_NODE_RESPONSE:
                writeFindNodeResponse((FindNodeResponse)msg);
                break;
            case DHTMessage.FIND_VALUE_REQUEST:
                writeFindValueRequest((FindValueRequest)msg);
                break;
            case DHTMessage.FIND_VALUE_RESPONSE:
                writeFindValueResponse((FindValueResponse)msg);
                break;
            case DHTMessage.STATS_REQUEST:
                writeStatsRequest((StatsRequest)msg);
                break;
            case DHTMessage.STATS_RESPONSE:
                writeStatsResponse((StatsResponse)msg);
                break;
            default:
                throw new IOException("Unknown Message: " + msg);
                
        }
    }
}
