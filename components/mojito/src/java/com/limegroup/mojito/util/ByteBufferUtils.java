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

package com.limegroup.mojito.util;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.security.CryptoHelper;

public final class ByteBufferUtils {
    
    public static byte[] getBytes(ByteBuffer buffer) {
        return getBytes(buffer, buffer.get() & 0xFF);
    }
    
    public static byte[] getBytes(ByteBuffer buffer, int length) {
        if (length == 0) {
            return null;
        }
        
        byte[] b = new byte[length];
        buffer.get(b);
        return b;
    }
    
    public static byte[] getSignature(ByteBuffer buffer, int length) {
        byte[] checksum = getBytes(buffer, length);
        if (ArrayUtils.isNull(checksum)) {
            return null;
        }
        return checksum;
    }
    
    public static SocketAddress getSocketAddress(ByteBuffer buffer) throws UnknownHostException {
        int length = buffer.get() & 0xFF;
        if (length == 0) {
            return null;
        }
        
        byte[] address = getBytes(buffer, length);
        int port = buffer.getShort() & 0xFFFF;
        
        return new InetSocketAddress(InetAddress.getByAddress(address), port);
    }
    
    public static QueryKey getQueryKey(ByteBuffer buffer) {
        int length = buffer.get() & 0xFF;
        if (length == 0) {
            return null;
        }
        
        return QueryKey.getQueryKey(getBytes(buffer, length), true);
    }
    
    public static ContactNode getContactNode(ByteBuffer buffer) throws UnknownHostException {
        KUID nodeId = KUID.createNodeID(buffer);
        SocketAddress addr = getSocketAddress(buffer);
        return new ContactNode(nodeId, addr);
    }
    
    public static List getContactNodes(ByteBuffer buffer) throws UnknownHostException {
        int size = buffer.get() & 0xFF;
        if (size == 0) {
            return Collections.EMPTY_LIST;
        }
        
        ContactNode[] nodes = new ContactNode[size];
        for(int i = 0; i < nodes.length; i++) {
            nodes[i] = getContactNode(buffer);
        }
        return Arrays.asList(nodes);
    }
    
    public static PublicKey getPublicKey(ByteBuffer buffer) {
        int length = buffer.getShort() & 0xFFFF;
        if (length == 0) {
            return null;
        }
        
        byte[] encodedKey = getBytes(buffer, length);
        return CryptoHelper.createPublicKey(encodedKey);
    }
    
    public static KeyValue getKeyValue(ByteBuffer buffer) throws UnknownHostException {
        KUID valueId = KUID.createValueID(buffer);
        
        int length = buffer.getShort() & 0xFFFF;
        byte[] value = getBytes(buffer, length);
        
        KUID nodeId = KUID.createNodeID(buffer);
        SocketAddress address = getSocketAddress(buffer);
        
        PublicKey pubKey = getPublicKey(buffer);
        byte[] signature = getBytes(buffer);
        
        return KeyValue.createRemoteKeyValue(valueId, value, nodeId, address, pubKey, signature);
    }
    
    public static List getKeyValues(ByteBuffer buffer) throws UnknownHostException {
        int size = buffer.get() & 0xFF;
        if (size == 0) {
            return Collections.EMPTY_LIST;
        }
        
        KeyValue[] values = new KeyValue[size];
        for(int i = 0; i < values.length; i++) {
            values[i] = getKeyValue(buffer);
        }
        return Arrays.asList(values);
    }
    
    public static String getUTFString(ByteBuffer buffer) throws UnsupportedEncodingException {
        int length = buffer.getInt();
        byte[] str = getBytes(buffer, length);
        return new String(str, "UTF-8");
    }
}
