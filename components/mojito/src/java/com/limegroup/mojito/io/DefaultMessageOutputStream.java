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
import java.io.OutputStream;

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

public class DefaultMessageOutputStream extends MessageOutputStream {

    public DefaultMessageOutputStream(OutputStream out) {
        super(out);
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
        writeSignature(request.getSecureSignature()); // TODO wrong!
        writeInt(request.getRequest());
    }
    
    private void writeStatsResponse(StatsResponse response) throws IOException {
        writeUTF(response.getStatistics());
    }
    
    public void write(DHTMessage msg) throws IOException {
        writeByte(msg.getOpCode());
        writeInt(msg.getVendor());
        writeShort(msg.getVersion());
        writeByte(msg.getContactNode().getFlags());
        writeKUID(msg.getContactNode().getNodeID());
        writeByte(msg.getContactNode().getInstanceID());
        writeKUID(msg.getMessageID());
        
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
