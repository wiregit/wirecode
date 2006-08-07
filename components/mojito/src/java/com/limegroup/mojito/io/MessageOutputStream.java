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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.DHTMessage.OpCode;
import com.limegroup.mojito.messages.StoreResponse.Status;

/**
 * The MessageOutputStream class writes a DHTMessage (serializes)
 * to a given OutputStream.
 */
public class MessageOutputStream extends DataOutputStream {
    
    public MessageOutputStream(OutputStream out) {
        super(out);
    }
	
    public void writeKUID(KUID kuid) throws IOException {
        if (kuid == null) {
            throw new NullPointerException("KUID cannot be null");
        }
        
        kuid.write(this);
    }
    
    public void writeMessageID(MessageID messageId) throws IOException {
        if (messageId == null) {
            throw new NullPointerException("MessageID cannot be null");
        }
        
        messageId.write(this);
    }
    
    public void writeDHTValue(DHTValue value) throws IOException {
        writeContact(value.getOriginator());
        value.getValueID().write(this);
        
        byte[] data = value.getData();
        writeShort(data.length);
        write(data, 0, data.length);
    }
    
    public void writeKUIDs(Collection<KUID> keys) throws IOException {
        writeByte(keys.size());
        for (KUID k : keys) {
            k.write(this);
        }
    }
    
    public void writeDHTValues(Collection<? extends DHTValue> values) throws IOException {
        writeByte(values.size());
        for(DHTValue value : values) {
            writeDHTValue(value);
        }
    }
    
    public void writePublicKey(PublicKey pubKey) throws IOException {
        if (pubKey != null) {
            byte[] encoded = pubKey.getEncoded();
            writeShort(encoded.length);
            write(encoded, 0, encoded.length);
        } else {
            writeShort(0);
        }
    }
    
    public void writeSignature(byte[] signature) throws IOException {
        if (signature != null && signature.length > 0) {
            writeByte(signature.length);
            write(signature, 0, signature.length);
        } else {
            writeByte(0);
        }
    }
    
    public void writeContact(Contact node) throws IOException {
        writeInt(node.getVendor());
        writeShort(node.getVersion());
        writeKUID(node.getNodeID());
        writeSocketAddress(node.getContactAddress());
    }
    
    public void writeContacts(Collection<? extends Contact> nodes) throws IOException {
        writeByte(nodes.size());
        for(Contact node : nodes) {
            writeContact(node);
        }
    }
    
    public void writeInetAddress(InetAddress addr) throws IOException {
        byte[] address = addr.getAddress();
        writeByte(address.length);
        write(address, 0, address.length);
    }
    
    public void writePort(int port) throws IOException {
        writeShort(port);
    }
    
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
    
    public void writeQueryKey(QueryKey queryKey) throws IOException {
        if (queryKey != null) {
            byte[] qk = queryKey.getBytes();
            writeByte(qk.length);
            write(qk, 0, qk.length);
        } else {
            writeByte(0);
        }
    }
    
    public void writeStatistics(byte[] statistics) throws IOException {
        if (statistics != null) {
            writeShort(statistics.length);
            write(statistics);
        } else {
            writeShort(0);
        }
    }
    
    public void writeOpCode(OpCode opcode) throws IOException {
        writeByte(opcode.getOpCode());
    }
    
    public void writeStoreStatus(Status status) throws IOException {
        writeByte(status.getStatus());
    }
}
