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
 
package de.kapsi.net.kademlia.db;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.security.CryptoHelper;

public class KeyValue implements Serializable {
    
    private static final long serialVersionUID = -666238398053901179L;
    
    private KUID key;
    private byte[] value;
    
    private KUID nodeId;
    private SocketAddress address;
    
    private boolean localKeyValue = false;
    
    private long creationTime;
    private long republishTime;
    
    private byte[] signature;
    
    private int hashCode = -1;
    
    private boolean isClose = true;
    
    public static KeyValue createLocalKeyValue(KUID key, byte[] value) {
        return createKeyValue(key, value, null, null, null, true);
    }
    
    public static KeyValue createLocalKeyValue(KUID key, byte[] value,
            KUID nodeId, SocketAddress address) {
        return createKeyValue(key, value, nodeId, address, null, true);
    }
    
    public static KeyValue createLocalKeyValue(KUID key, byte[] value, ContactNode node) {
        return createKeyValue(key, value, 
                node.getNodeID(), node.getSocketAddress(), null, true);
    }
    
    public static KeyValue createRemoteKeyValue(KUID key, byte[] value, 
            KUID nodeId, SocketAddress address, byte[] signature) {
        return createKeyValue(key, value, nodeId, address, signature, false);
    }
    
    private static KeyValue createKeyValue(KUID key, byte[] value, 
            KUID nodeId, SocketAddress address, byte[] signature, 
            boolean localKeyValue) {
        
        return new KeyValue(key, value, nodeId, address, signature, localKeyValue);
    }
    
    private KeyValue(KUID key, byte[] value, 
            KUID nodeId, SocketAddress address, byte[] signature, 
            boolean localKeyValue) {
        
        this.key = key;
        this.value = value;
        
        if (nodeId != null) {
            this.nodeId = nodeId;
        } else {
            this.nodeId = KUID.createUnknownID();
        }
        
        this.address = address;
        
        this.signature = signature;
        
        this.creationTime = System.currentTimeMillis();
        this.localKeyValue = localKeyValue;
    }
    
    public boolean isAnonymous() {
        return nodeId == null || address == null 
                    || nodeId.isUnknownID();
    }
    
    public boolean isLocalKeyValue() {
        return localKeyValue;
    }
    
    public boolean verify(PublicKey pubKey) 
            throws SignatureException, InvalidKeyException {
        
        return pubKey != null && signature != null 
                    && CryptoHelper.verify(pubKey, value, signature);
    }
    
    public void setClose(boolean close) {
        this.isClose = close;
    }
    
    public boolean isClose() {
        return isClose;
    }
    
    public KUID getKey() {
        return key;
    }
    
    public byte[] getValue() {
        return value;
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    public InetAddress getInetAddress() {
        if (address == null) {
            return null;
        }
        return ((InetSocketAddress)address).getAddress();
    }
    
    public int getPort() {
        if (address == null) {
            return -1;
        }
        return ((InetSocketAddress)address).getPort();
    }
    
    public boolean isSigned() {
        return signature != null;
    }
    
    public byte[] getSignature() {
        return signature;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
    
    public long getRepublishTime() {
        return republishTime;
    }
    
    public void setRepublishTime(long republishTime) {
        this.republishTime = republishTime;
    }
    
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = key.hashCode() ^ value.hashCode();
            if (hashCode == -1) {
                hashCode = 0;
            }
        }
        return hashCode;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof KeyValue)) {
            return false;
        }
        
        KeyValue v = (KeyValue)o;
        if (!key.equals(v.key)) {
            return false;
        }
        
        if (nodeId != null && !nodeId.equals(v.nodeId)) {
            return false;
        }
        
        if (address != null && !address.equals(v.address)) {
            return false;
        }
        
        return Arrays.equals(value, v.value);
    }
    
    public String toString() {
        return "KeyValue: " + key.toString() + " = " + new String(value) + " - originator: "+nodeId;
    }
}
