/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.db;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.security.CryptoHelper;
import de.kapsi.net.kademlia.security.SignatureVerificationException;

public class KeyValue {
    
    public static final int STANDARD    = 0x01;
    public static final int CLEAR       = 0x02;
    public static final int LOCK        = 0x04;
    
    private KUID key;
    private byte[] value;
    
    private long creationTime;
    private long updateTime;
    
    private PublicKey pubKey;
    private byte[] signature;
    
    private int mode = STANDARD;
    
    private int hashCode = -1;
    
    public KeyValue(KUID key, byte[] value, 
        KeyPair keyPair, long creationTime, int mode) 
            throws SignatureException, InvalidKeyException {
        
        this.key = key.assertValueID();
        this.value = value;
        this.pubKey = keyPair.getPublic();
        this.signature = CryptoHelper.sign(keyPair, value);
        this.creationTime = creationTime;
        this.mode = mode;
        
        this.updateTime = 0L;
    }
    
    public KeyValue(KUID key, byte[] value, 
        PublicKey pubKey, byte[] signature, 
        long creationTime, int mode)
            throws SignatureException, InvalidKeyException {
        
        this.key = key.assertValueID();
        this.value = value;
        this.pubKey = pubKey;
        this.signature = signature;
        this.creationTime = creationTime;
        this.mode = mode;
        this.updateTime = System.currentTimeMillis();
        
        if (!verify()) {
            throw new SignatureVerificationException();
        }
    }
    
    public boolean verify() 
            throws SignatureException, InvalidKeyException {
        return verify(pubKey);
    }
    
    public boolean verify(PublicKey pubKey) 
            throws SignatureException, InvalidKeyException {
        return CryptoHelper.verify(pubKey, value, signature);
    }
    
    public KUID getKey() {
        return key;
    }
    
    public byte[] getValue() {
        return value;
    }
    
    public PublicKey getPublicKey() {
        return pubKey;
    }
    
    public byte[] getSignature() {
        return signature;
    }
    
    public boolean isStandard() {
        return (mode & STANDARD) == STANDARD;
    }
    
    public boolean isClear() {
        return (mode & CLEAR) == CLEAR;
    }
    
    public boolean isLock() {
        return (mode & LOCK) == LOCK;
    }
    
    public int getMode() {
        return mode;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    // TODO creationTime must be also signed with the
    // Private Key!!!
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
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
        
        return Arrays.equals(value, v.value);
    }
    
    public String toString() {
        return key.toString() + " = " + new String(value);
    }
}
