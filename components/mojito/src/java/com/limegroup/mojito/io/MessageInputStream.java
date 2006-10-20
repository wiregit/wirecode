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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueFactory;
import com.limegroup.mojito.db.DHTValue.ValueType;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.DHTMessage.OpCode;
import com.limegroup.mojito.messages.StatsRequest.Type;
import com.limegroup.mojito.messages.StoreResponse.Status;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.ContactFactory;
import com.limegroup.mojito.util.EntryImpl;

/**
 * The MessageInputStream reads (parses) a DHTMessage
 * from a given InputStream
 */
public class MessageInputStream extends DataInputStream {
    
    public MessageInputStream(InputStream in) {
        super(in);
    }
    
    /**
     * Reads the given number of bytes
     */
    public byte[] readBytes(int bytes) throws IOException {
        byte[] buf = new byte[bytes];
        readFully(buf);
        return buf;
    }
    
    /**
     * Reads a KUID from the InputStream 
     */
    public KUID readKUID() throws IOException {
        return KUID.create(readBytes(KUID.LENGTH));
    }
    
    /**
     * Reads a MessageID from the InputStream 
     */
    public MessageID readMessageID() throws IOException {
        return MessageID.createWithBytes(readBytes(MessageID.LENGTH));
    }
    
    /**
     * Reads a BigInteger from the InputStream 
     */
    public BigInteger readDHTSize() throws IOException {
        int length = readUnsignedByte();
        if (length > KUID.LENGTH) { // can't be more than 2**160 bit
            throw new IOException("Illegal length: " + length);
        }
        
        byte[] num = readBytes(length);
        return new BigInteger(1 /* unsigned */, num);
    }
    
    /**
     * Reads a DHTValue from the InputStream 
     * 
     * @param sender The Contact that send us the DHTValue
     */
    public DHTValue readDHTValue(Contact sender) throws IOException {
        Contact originator = readContact();
        KUID valueId = readKUID();
        ValueType type = readValueType();
        
        byte[] data = null;
        int length = readUnsignedShort();
        if (length > 0) {
            data = new byte[length];
            readFully(data);
        }
        
        return DHTValueFactory.createRemoteValue(originator, sender, valueId, type, data);
    }
    
    /**
     * Reads multiple DHTValues from the InputStream 
     */
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
    
    /**
     * Reads multiple KUIDs from the InputStream 
     */
    public Collection<KUID> readKUIDs() throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.emptySet();
        }
        
        KUID[] keys = new KUID[size];
        for (int i = 0; i < size; i++) {
            keys[i] = readKUID();
        }
        
        return Arrays.asList(keys);
    }
    
    /**
     * Reads a Signature from the InputStream  
     */
    public byte[] readSignature() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] signature = new byte[length];
        readFully(signature, 0, signature.length);
        return signature;
    }
	
    /**
     * Reads a Contact from the InputStream 
     */
    public Contact readContact() throws IOException {
        int vendor = readInt();
        int version = readUnsignedShort();
        KUID nodeId = readKUID();
        SocketAddress addr = readSocketAddress();
        
        if (addr == null) {
            throw new UnknownHostException("SocketAddress is null");
        }
        
        return ContactFactory.createUnknownContact(vendor, version, nodeId, addr);
    }
    
    /**
     * Reads multiple Contacts from the InputStream 
     */
    public Collection<Contact> readContacts() throws IOException {
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

    /**
     * Reads an InetAddress from the InputStream 
     */
    public InetAddress readInetAddress() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] address = new byte[length];
        readFully(address);
        
        return InetAddress.getByAddress(address);
    }
    
    /**
     * Reads a Port number from the InputStream 
     */
    public int readPort() throws IOException {
        return readUnsignedShort();
    }
    
    /**
     * Reads a SocketAddress from the InputStream 
     */
    public InetSocketAddress readSocketAddress() throws IOException {
        InetAddress addr = readInetAddress();
        if (addr == null) {
            return null;
        }
        
        int port = readPort();
        return new InetSocketAddress(addr, port);
    }
    
    /**
     * Reads a QueryKey from the InputStream 
     */
    public QueryKey readQueryKey() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] queryKey = new byte[length];
        readFully(queryKey, 0, queryKey.length);
        return QueryKey.getQueryKey(queryKey, true);
    }
    
    /**
     * Reads an encoded Statistics 
     */
    public byte[] readStatistics() throws IOException {
        int length = readUnsignedShort();
        if (length == 0) {
            return null;
        }
        
        byte[] statistics = new byte[length];
        readFully(statistics);
        return statistics;
    }
    
    /**
     * Reads an OpCode from the InputStream
     */
    public OpCode readOpCode() throws IOException {
        return OpCode.valueOf(readUnsignedByte());
    }
    
    /**
     * Reads an Type from the InputStream
     */
    public Type readStatsType() throws IOException {
        return Type.valueOf(readUnsignedByte());
    }
    
    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public Collection<Entry<KUID, Status>> readStoreStatus() throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.emptyList();
        }
        
        Entry<KUID, Status>[] entries = new Entry[size];
        for (int i = 0; i < entries.length; i++) {
            KUID valueId = readKUID();
            Status status = Status.valueOf( readUnsignedByte() );
            entries[i] = new EntryImpl<KUID, Status>(valueId, status);
        }
        return Arrays.asList(entries);
    }
    
    /***
     * 
     */
    public ValueType readValueType() throws IOException {
        return ValueType.valueOf(readInt());
    }
}
