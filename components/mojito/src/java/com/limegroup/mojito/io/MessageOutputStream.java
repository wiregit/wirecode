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

/**
 * The MessageOutputStream class writes a DHTMessage (serializes)
 * to a given OutputStream.
 */
public class MessageOutputStream extends DataOutputStream {
    
    public MessageOutputStream(OutputStream out) {
        super(out);
    }
	
    public void writeKUID(KUID key) throws IOException {
        if (key == null) {
            throw new NullPointerException("KUID cannot be null");
        }
        
        write(key.getBytes());
    }
    
    public void writeKeyValue(KeyValue keyValue) throws IOException {
        writeKUID((KUID)keyValue.getKey());
        byte[] b = (byte[])keyValue.getValue();
        writeShort(b.length);
        write(b, 0, b.length);
        
        writeKUID(keyValue.getNodeID());
        writeSocketAddress(keyValue.getSocketAddress());
        
        writePublicKey(keyValue.getPublicKey());
        writeSignature(keyValue.getSignature());
    }
    
    public void writeKeyValues(Collection values) throws IOException {
        writeByte(values.size());
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            writeKeyValue((KeyValue)it.next());
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
    
    public void writeContactNode(ContactNode node) throws IOException {
        writeKUID(node.getNodeID());
        writeSocketAddress(node.getSocketAddress());
	}
    
    public void writeContactNodes(Collection nodes) throws IOException {
        writeByte(nodes.size());
        for(Iterator it = nodes.iterator(); it.hasNext(); ) {
            writeContactNode((ContactNode)it.next());
        }
    }
    
    public void writeSocketAddress(SocketAddress addr) throws IOException {
        if (addr instanceof InetSocketAddress
                && !((InetSocketAddress)addr).isUnresolved()) {
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
    
    public void writeQueryKey(QueryKey queryKey) throws IOException {
        if (queryKey != null) {
            byte[] qk = queryKey.getBytes();
            writeByte(qk.length);
            write(qk, 0, qk.length);
        } else {
            writeByte(0);
        }
    }
}
