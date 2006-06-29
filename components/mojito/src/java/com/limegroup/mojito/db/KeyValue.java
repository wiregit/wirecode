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
 
package com.limegroup.mojito.db;

import java.io.Serializable;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Map;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.security.CryptoHelper;
import com.limegroup.mojito.util.ContactUtils;

/**
 * The KeyValue class is essentially a <Key, Value> Tuple with
 * some additional fields like originator, PublicKey or the 
 * signature of the KeyValue.
 */
public class KeyValue implements Map.Entry<KUID, byte[]>, Serializable {
    
    private static final long serialVersionUID = -666238398053901179L;
    
    private KUID key;
    private byte[] value;
    
    private KUID nodeId;
    private SocketAddress address;
    
    private PublicKey pubKey;
    private byte[] signature;
    
    private boolean localKeyValue = false;
    
    private long creationTime;
    private long lastPublishTime;
    
    private int hashCode = -1;
    
    private boolean nearby = true;
    
    /**
     * The number of locations we have stored this value at
     */
    private int numLocs;
    
    /** Constructs an anonymous local KeyValue */
    public static KeyValue createLocalKeyValue(KUID key, byte[] value) {
        return createKeyValue(key, value, null, null, null, null, true);
    }
    
    /** Constrcuts a local KeyValue */
    public static KeyValue createLocalKeyValue(KUID key, byte[] value, 
            Contact node) {
        return createKeyValue(key, value, 
                node.getNodeID(), node.getSocketAddress(), null, null, true);
    }
    
    /** Constructs an local KeyValue */
    public static KeyValue createLocalKeyValue(KUID key, byte[] value,
            KUID nodeId, SocketAddress address) {
        return createKeyValue(key, value, nodeId, address, null, null, true);
    }
    
    /** Constructs a remote KeyValue we've received from the DHT */
    public static KeyValue createRemoteKeyValue(KUID key, byte[] value, 
            KUID nodeId, SocketAddress address, PublicKey pubKey, byte[] signature) {
        return createKeyValue(key, value, nodeId, address, pubKey, signature, false);
    }
    
    /** Helper method to construct local as well as remote KeyValues */
    private static KeyValue createKeyValue(KUID key, byte[] value, 
            KUID nodeId, SocketAddress address, PublicKey pubKey, byte[] signature, 
            boolean localKeyValue) {
        
        return new KeyValue(key, value, nodeId, address, pubKey, signature, localKeyValue);
    }
    
    /** Private, use static constructors! */
    private KeyValue(KUID key, byte[] value, 
            KUID nodeId, SocketAddress address, 
            PublicKey pubKey, byte[] signature, 
            boolean localKeyValue) {
        
        this.key = key;
        this.value = value;
        
        if (nodeId != null) {
            this.nodeId = nodeId;
        } else {
            this.nodeId = KUID.createUnknownID();
        }
        
        this.address = address;
        
        this.pubKey = pubKey;
        this.signature = signature;
        
        this.creationTime = System.currentTimeMillis();
        this.localKeyValue = localKeyValue;
    }
    
    /**
     * A KeyValue is anonymous if the nodeId or the 
     * address of the originator is unknown.
     */
    public boolean isAnonymous() {
        return nodeId == null || address == null 
                    || nodeId.isUnknownID();
    }
    
    /**
     * Returns whether or not the KeyValue is a local KeyValue
     */
    public boolean isLocalKeyValue() {
        return localKeyValue;
    }
    
    /**
     * Signs the KeyValue with the PrivateKey
     */
    public void sign(PrivateKey privateKey) 
            throws InvalidKeyException, SignatureException {
        signature = CryptoHelper.sign(privateKey, key.getBytes(), value);
    }
    
    /**
     * Verifies the KeyValue/Signature with this KeyValues
     * PublicKey
     */
    public boolean verify()
            throws SignatureException, InvalidKeyException {
        return verify(getPublicKey());
    }
    
    /**
     * Verifies the KeyValue/Signature with the given
     * PublicKey
     */
    public boolean verify(PublicKey pubKey) 
            throws SignatureException, InvalidKeyException {
        if (pubKey != null && signature != null) {
            return CryptoHelper.verify(pubKey, signature, key.getBytes(), value);
        }
        return false;
    }
    
    /**
     * Sets the PublicKey of this KeyValue.
     * 
     * Note: The KeyValue must be signed and the signature
     * must match with the given PublicKey or an IllegalArgumentException
     * will be thrown!
     */
    public void setPublicKey(PublicKey pubKey) 
            throws SignatureException, InvalidKeyException {
        
        if (!verify(pubKey)) {
            throw new IllegalArgumentException("Signature and PublicKey don't match");
        }
        this.pubKey = pubKey;
    }
    
    /**
     * Sets whether or not the KeyValue is considered
     * as nearby to our local NodeID
     */
    public void setNearby(boolean nearby) {
        this.nearby = nearby;
    }
    
    /**
     * Returns whether or not the KeyValue is considered
     * as nearby to our local NodeID
     */
    public boolean isNearby() {
        return nearby;
    }
    
    /**
     * Returns the Key of this KeyValue
     */
    public KUID getKey() {
        return key;
    }
    
    /**
     * Returns the Value of this KeyValue.
     */
    public byte[] getValue() {
        return value;
    }
    
    /**
     * Not implemented, throws an UnsupportedOperationException!
     */
    public byte[] setValue(byte[] value) {
        throw new UnsupportedOperationException("This class is immutable");
    }
    
    /** 
     * Returns whether or not the value is "empty". Storing
     * empty values on the DHT means removing them.
     */
    public boolean isEmptyValue() {
        return value == null || value.length == 0;
    }
    
    /**
     * Returns the NodeID of the originator. In case of
     * anonymous KeyValues it's type of an unknown ID
     * rather than a true Node ID.
     */
    public KUID getNodeID() {
        return nodeId;
    }
    
    /**
     * Returns the SocketAddress of the originator or
     * null if the originator is unknown.
     */
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    /**
     * Returns whether or not the KeyValue is signed.
     */
    public boolean isSigned() {
        return signature != null;
    }
    
    /**
     * Returns the PublicKey or null if no key is set
     */
    public PublicKey getPublicKey() {
        return pubKey;
    }
    
    /**
     * Returns the signature or null if the KeyValue is not signed.
     */
    public byte[] getSignature() {
        return signature;
    }
    
    /**
     * Returns the creation time of the KeyValue.
     * 
     * Note: Creation time is the time when this object
     * was created.
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Sets the creation time of this KeyValue object
     */
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
    
    /**
     * Returns the time when this KeyValue was published last time
     */
    public long getLastPublishTime() {
        return lastPublishTime;
    }
    
    /**
     * Sets the publish time of this KeyValue object
     */
    void setLastPublishTime(long publishTime) {
        this.lastPublishTime = publishTime;
    }
    
    /**
     * Returns the number of locations where this 
     * KeyValue has been stored
     */
    public int getNumLocs() {
        return numLocs;
    }

    /**
     * Sets the number of locations where this 
     * KeyValue has been stored
     */
    public void setNumLocs(int numLocs) {
        this.numLocs = numLocs;
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
        if (!isEmptyValue()) {
            return key.toString() + " = " + new String(value) 
                + ", originator=" + ContactUtils.toString(nodeId, address);
        } else {
            return key.toString() + ", originator=" + ContactUtils.toString(nodeId, address) + " (REMOVE)";
        }
    }
}
