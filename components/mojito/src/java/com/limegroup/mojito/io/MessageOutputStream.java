/*
 * Mojito Distributed Hash Table (Mojito DHT)
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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValue.ValueType;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.DHTMessage.OpCode;
import com.limegroup.mojito.messages.StatsRequest.Type;
import com.limegroup.mojito.messages.StoreResponse.Status;
import com.limegroup.mojito.routing.Contact;

/**
 * The MessageOutputStream class writes a DHTMessage (serializes)
 * to a given OutputStream.
 */
public class MessageOutputStream extends DataOutputStream {
    
    public MessageOutputStream(OutputStream out) {
        super(out);
    }
	
    /**
     * Writes the given KUID to the OutputStream
     */
    public void writeKUID(KUID kuid) throws IOException {
        if (kuid == null) {
            throw new NullPointerException("KUID cannot be null");
        }
        
        kuid.write(this);
    }
    
    /**
     * Writes the given MessageID to the OutputStream
     */
    public void writeMessageID(MessageID messageId) throws IOException {
        if (messageId == null) {
            throw new NullPointerException("MessageID cannot be null");
        }
        
        messageId.write(this);
    }
    
    /**
     * Writes the given BigInteger to the OutputStream
     */
    public void writeDHTSize(BigInteger estimatedSize) throws IOException {
        byte[] data = estimatedSize.toByteArray();
        if (data.length > KUID.LENGTH) { // Can't be more than 2**160 bit
            throw new IOException("Illegal length: " + data.length + "/" + estimatedSize);
        }
        writeByte(data.length);
        write(data, 0, data.length);
    }
    
    /**
     * Writes the given DHTValue to the OutputStream
     */
    public void writeDHTValue(DHTValue value) throws IOException {
        writeContact(value.getOriginator());
        value.getValueID().write(this);
        
        ValueType type = value.getValueType();
        writeInt(type.toInt());
        
        byte[] data = value.getData();
        writeShort(data.length);
        write(data, 0, data.length);
    }
    
    /**
     * Writes the given Collection of KUIDs to the OutputStream
     */
    public void writeKUIDs(Collection<KUID> keys) throws IOException {
        writeCollectionSize(keys);
        for (KUID k : keys) {
            k.write(this);
        }
    }
    
    /**
     * Writes the given Collection of DHTValues to the OutputStream
     */
    public void writeDHTValues(Collection<? extends DHTValue> values) throws IOException {
        writeCollectionSize(values);
        for(DHTValue value : values) {
            writeDHTValue(value);
        }
    }
    
    /**
     * Writes the given Signature to the OutputStream
     */
    public void writeSignature(byte[] signature) throws IOException {
        if (signature != null && signature.length > 0) {
            writeByte(signature.length);
            write(signature, 0, signature.length);
        } else {
            writeByte(0);
        }
    }
    
    /**
     * Writes the given Contact to the OutputStream
     */
    public void writeContact(Contact node) throws IOException {
        writeInt(node.getVendor());
        writeShort(node.getVersion());
        writeKUID(node.getNodeID());
        writeSocketAddress(node.getContactAddress());
    }
    
    /**
     * Writes the given Collection of Contact to the OutputStream
     */
    public void writeContacts(Collection<? extends Contact> nodes) throws IOException {
        writeCollectionSize(nodes);
        for(Contact node : nodes) {
            writeContact(node);
        }
    }
    
    /**
     * Writes the given InetAddress to the OutputStream
     */
    public void writeInetAddress(InetAddress addr) throws IOException {
        byte[] address = addr.getAddress();
        writeByte(address.length);
        write(address, 0, address.length);
    }
    
    /**
     * Writes the given Port number to the OutputStream
     */
    public void writePort(int port) throws IOException {
        writeShort(port);
    }
    
    /**
     * Writes the given SocketAddress to the OutputStream
     */
    public void writeSocketAddress(SocketAddress addr) throws IOException {
        if (addr instanceof InetSocketAddress
                && !((InetSocketAddress)addr).isUnresolved()) {
            InetSocketAddress iaddr = (InetSocketAddress)addr;
            
            writeInetAddress(iaddr.getAddress());
            writePort(iaddr.getPort());
        } else {
            writeByte(0);
        }
    }
    
    /**
     * Writes the given QueryKey to the OutputStream
     */
    public void writeQueryKey(QueryKey queryKey) throws IOException {
        if (queryKey != null) {
            byte[] qk = queryKey.getBytes();
            writeByte(qk.length);
            write(qk, 0, qk.length);
        } else {
            writeByte(0);
        }
    }
    
    /**
     * Writes an encoded Statistics payload to the OutputStream
     */
    public void writeStatistics(byte[] statistics) throws IOException {
        if (statistics != null) {
            writeShort(statistics.length);
            write(statistics);
        } else {
            writeShort(0);
        }
    }
    
    /**
     * Writes the given OpCode to the OutputStream
     */
    public void writeOpCode(OpCode opcode) throws IOException {
        writeByte(opcode.toByte());
    }
    
    /**
     * Writes the given Type to the OutputStream
     */
    public void writeStatsType(Type type) throws IOException {
        writeByte(type.toByte());
    }
    
    /**
     * Writes the given Contact to the OutputStream
     */
    public void writeStoreStatus(Collection<? extends Entry<KUID, Status>> status) throws IOException {
        writeCollectionSize(status);
        for (Entry<KUID, Status> e : status) {
            writeKUID(e.getKey());
            writeByte(e.getValue().toByte());
        }
    }
    
    /**
     * Writes the size of the given Collection to the OutputStream
     */
    private void writeCollectionSize(Collection c) throws IOException {
        int size = c.size();
        if (size > 0xFF) {
            throw new IOException("Too many elements: " + size);
        }
        
        writeByte(size);
    }
}
