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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.StoreResponse.StoreStatus;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.security.CryptoHelper;

/**
 * The MessageInputStream reads (parses) a DHTMessage
 * from a given InputStream
 */
public class MessageInputStream extends DataInputStream {
    
    public MessageInputStream(InputStream in) {
        super(in);
    }
    
    public byte[] readBytes(int bytes) throws IOException {
        byte[] buf = new byte[bytes];
        readFully(buf);
        return buf;
    }
    
    private byte[] readKUIDBytes() throws IOException {
        return readBytes(KUID.LENGTH);
    }
    
    public KUID readNodeID() throws IOException {
        return KUID.createNodeID(readKUIDBytes());
    }
    
    public MessageID readMessageID() throws IOException {
        return MessageID.create(readBytes(MessageID.LENGTH));
    }
    
    public KUID readValueID() throws IOException {
        return KUID.createValueID(readKUIDBytes());
    }
    
    public DHTValue readDHTValue(Contact sender) throws IOException {
        Contact originator = readContact();
        KUID valueId = readValueID();
        byte[] data = null;
        int length = readUnsignedShort();
        if (length > 0) {
            data = new byte[length];
            readFully(data);
        }
        
        return DHTValue.createRemoteValue(originator, sender, valueId, data);
    }
    
    public List<DHTValue> readDHTValues(Contact sender) throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.emptyList();
        }
        
        DHTValue[] values = new DHTValue[size];
        for(int i = 0; i < values.length; i++) {
            values[i] = readDHTValue(sender);
        }
        return Arrays.asList(values);
    }
    
    public Collection<KUID> readKUIDs() throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.emptySet();
        }
        
        KUID[] keys = new KUID[size];
        for (int i = 0; i < size; i++) {
            keys[i] = readValueID();
        }
        
        return Arrays.asList(keys);
    }
    
    public PublicKey readPublicKey() throws IOException {
        int length = readUnsignedShort();
        if (length == 0) {
            return null;
        }
        
        byte[] encodedKey = new byte[length];
        readFully(encodedKey);
        return CryptoHelper.createPublicKey(encodedKey);
    }
    
    public byte[] readSignature() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] signature = new byte[length];
        readFully(signature, 0, signature.length);
        return signature;
    }
	
    public Contact readContact() throws IOException {
        int vendor = readInt();
        int version = readUnsignedShort();
        KUID nodeId = readNodeID();
        SocketAddress addr = readSocketAddress();
        
        if (addr == null) {
            throw new UnknownHostException("SocketAddress is null");
        }
        
        return ContactNode.createUnknownContact(vendor, version, nodeId, addr);
    }
    
    public List<Contact> readContacts() throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.emptyList();
        }
        
        Contact[] nodes = new Contact[size];
        for(int i = 0; i < nodes.length; i++) {
            nodes[i] = readContact();
        }
        return Arrays.asList(nodes);
    }

    public InetAddress readInetAddress() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] address = new byte[length];
        readFully(address);
        
        return InetAddress.getByAddress(address);
    }
    
    public int readPort() throws IOException {
        return readUnsignedShort();
    }
    
    public InetSocketAddress readSocketAddress() throws IOException {
        InetAddress addr = readInetAddress();
        if (addr == null) {
            return null;
        }
        
        int port = readPort();
        return new InetSocketAddress(addr, port);
    }
    
    public QueryKey readQueryKey() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] queryKey = new byte[length];
        readFully(queryKey, 0, queryKey.length);
        return QueryKey.getQueryKey(queryKey, true);
    }
    
    public byte[] readStatistics() throws IOException {
        int length = readUnsignedShort();
        if (length == 0) {
            return null;
        }
        
        byte[] statistics = new byte[length];
        readFully(statistics);
        return statistics;
    }
    
    public StoreStatus readStoreStatus() throws IOException {
        return StoreStatus.valueOf( readUnsignedByte() );
    }
}
