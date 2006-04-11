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
 
package de.kapsi.net.kademlia;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;

import de.kapsi.net.kademlia.util.ArrayUtils;
import de.kapsi.net.kademlia.util.PatriciaTrie.KeyCreator;

/**
 * KUID stands for Kademlia Unique Identifier and represents 
 * an 160bit value. You're welcome to proposal a better name 
 * which is suitable for Keys, NodeIDs and MessageIDs!
 * 
 * This class is immutable!
 * 
 * TODO: Maybe Key, NodeID and MessageID but 'Key' sucks!
 */
public class KUID implements Serializable {
    
    private static final long serialVersionUID = 633717248208386374L;

    public static final int LENGTH = 160; // bit
    
    private static final Random GENERATOR = new Random();
    
    private static final int[] BITS = {
        0x80,
        0x40,
        0x20,
        0x10,
        0x8,
        0x4,
        0x2,
        0x1
    };
    
    public static final int UNKNOWN_ID = 0x01;
    public static final int NODE_ID = 0x02;
    public static final int VALUE_ID = 0x03;
    public static final int MESSAGE_ID = 0x04;
    
    /** All bits 0 Unknown ID */
    public static final KUID MIN_UNKNOWN_ID;
    
    /** All bits 1 Unknown ID */
    public static final KUID MAX_UNKNOWN_ID;
    
    /** All bits 0 Node ID */
    public static final KUID MIN_NODE_ID;
    
    /** All bits 1 Node ID */
    public static final KUID MAX_NODE_ID;
    
    /** All bits 0 Value ID */
    public static final KUID MIN_VALUE_ID;
    
    /** All bits 1 Value ID */
    public static final KUID MAX_VALUE_ID;
    
    /** All bits 0 Message ID */
    public static final KUID MIN_MESSAGE_ID;
    
    /** All bits 1 Message ID */
    public static final KUID MAX_MESSAGE_ID;
    
    static {
        byte[] min = new byte[20];
        
        byte[] max = new byte[20];
        Arrays.fill(max, (byte)0xFF);
        
        MIN_UNKNOWN_ID = new KUID(UNKNOWN_ID, min);
        MAX_UNKNOWN_ID = new KUID(UNKNOWN_ID, max);
        
        MIN_NODE_ID = new KUID(NODE_ID, min);
        MAX_NODE_ID = new KUID(NODE_ID, max);
        
        MIN_VALUE_ID = new KUID(VALUE_ID, min);
        MAX_VALUE_ID = new KUID(VALUE_ID, max);
        
        MIN_MESSAGE_ID = new KUID(MESSAGE_ID, min);
        MAX_MESSAGE_ID = new KUID(MESSAGE_ID, max);
    }
    
    protected final int type;
    protected final byte[] id;
    
    private int hashCode;
    
    protected KUID(int type, byte[] id) {
        if (id == null) {
            throw new NullPointerException("ID is null");
        }
        
        if ((id.length * 8) != LENGTH) {
            throw new IllegalArgumentException("ID must be " + LENGTH + " bits long");
        }
        
        this.type = type;
        this.id = id;
        this.hashCode = ArrayUtils.hashCode(id);
    }
    
    /**
     * Returns the identity and throws a RuntimeException
     * if this isn't a Node ID.
     */
    public KUID assertNodeID() throws RuntimeException {
        if (!isNodeID()) {
            throw new RuntimeException(this + " is not a NODE_ID");
        }
        return this;
    }
    
    /**
     * Returns true if this is a Node ID
     */
    public boolean isNodeID() {
        return type == NODE_ID;
    }
    
    /**
     * Returns the identity and throws a RuntimeException
     * if this isn't a Value ID.
     */
    public KUID assertValueID() throws RuntimeException {
        if (!isValueID()) {
            throw new RuntimeException(this + " is not a VALUE_ID");
        }
        return this;
    }
    
    /**
     * Returns true if this is a Value ID
     */
    public boolean isValueID() {
        return type == VALUE_ID;
    }
    
    /**
     * Returns the identity and throws a RuntimeException
     * if this isn't a Message ID.
     */
    public KUID assertMessageID() throws RuntimeException {
        if (!isMessageID()) {
            throw new RuntimeException(this + " is not a MESSAGE_ID");
        }
        return this;
    }
    
    /**
     * Returns true if this is a Message ID
     */
    public boolean isMessageID() {
        return type == MESSAGE_ID;
    }
    
    /**
     * Returns true if this is a Unknown ID
     */
    public boolean isUnknownID() {
        return type == UNKNOWN_ID;
    }
    
    public boolean isBitSet(int bitIndex) {
        int index = (int) (bitIndex / BITS.length);
        int bit = (int) (bitIndex - index * BITS.length);
        return (id[index] & BITS[bit]) != 0;
    }
    
    /**
     * Sets the specified bit to 1 and returns a new
     * KUID instance
     */
    public KUID set(int bit) {
        return set(bit, true);
    }
    
    /**
     * Sets the specified bit to 0 and returns a new
     * KUID instance
     */
    public KUID unset(int bit) {
        return set(bit, false);
    }
    
    private KUID set(int bitIndex, boolean set) {
        int index = (int) (bitIndex / BITS.length);
        int bit = (int) (bitIndex - index * BITS.length);
        boolean isBitSet = (id[index] & BITS[bit]) != 0;
        
        // Don't create a new Object if nothing is
        // gonna change
        if (isBitSet != set) {
            byte[] id = getBytes();
            if (set) {
                id[index] |= BITS[bit];
            } else {
                id[index] &= ~BITS[bit];
            }
            return new KUID(type, id);
        } else {
            return this;
        }
    }
    
    /**
     * Returns the number of bits that are 1
     */
    public int bits() {
        int bits = 0;
        for(int i = 0; i < LENGTH; i++) {
            if (isBitSet(i)) {
                bits++;
            }
        }
        return bits;
    }
    
    public int bitIndex(KUID nodeId) {
        boolean allNull = true;
        
        int bitIndex = 0;
        for(int i = 0; i < id.length; i++) {
            if (allNull && id[i] != 0) {
                allNull = false;
            }
            
            if (id[i] != nodeId.id[i]) {
                for(int j = 0; j < BITS.length; j++) {
                    if ((id[i] & BITS[j]) 
                            != (nodeId.id[i] & BITS[j])) {
                        break;
                    }
                    bitIndex++;
                }
                break;
            }
            bitIndex += BITS.length;
        }
        
        if (allNull) {
            return KeyCreator.NULL_BIT_KEY;
        }
        
        if (bitIndex == LENGTH) {
            return KeyCreator.EQUAL_BIT_KEY;
        }
        
        return bitIndex;
    }
    
    /**
     * Returns the xor distance between the current and passed KUID.
     */
    public KUID xor(KUID nodeId) {
        byte[] result = new byte[id.length];
        for(int i = 0; i < result.length; i++) {
            result[i] = (byte)(id[i] ^ nodeId.id[i]);
        }
        
        int t = (type == nodeId.type) ? type : UNKNOWN_ID;
        return new KUID(t, result);
    }
    
    /**
     * Inverts all bits of the current KUID
     */
    public KUID invert() {
        byte[] result = new byte[id.length];
        for(int i = 0; i < result.length; i++) {
            result[i] = (byte)~id[i];
        }
        return new KUID(type, result);
    }
    
    /**
     * Computes the closest address to targetID between this KUID and nodeID
     * 
     * @param nodeID the KUID to compare to
     * @param targetID the target ID
     * @return true if this is closer to targetID, false otherwise
     */
    public boolean isCloser(KUID nodeID, KUID targetID) {
        
        for (int i=0;i<id.length;i++){

            int dSelf = (id[i] ^ targetID.id[i]) & 0xFF;
            int dOther = (nodeID.id[i] ^ targetID.id[i]) & 0xFF;
            int diff = dOther - dSelf;
            if ( diff > 0 ){
                return true;
            } else if (diff < 0 ) {
                return false;
            } 
        }
        return false;
    }
    
    /**
     * Returns the type of the current KUID
     */
    public int getType() {
        return type;
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    /**
     * Returns the raw bytes of the current KUID
     */
    public byte[] getBytes() {
        byte[] clone = new byte[id.length];
        System.arraycopy(id, 0, clone, 0, id.length);
        return clone;
    }
    
    /**
     * KUIDs are equal if they're of the same type and 
     * if their IDs are equal.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } if (!(o instanceof KUID)) {
            return false;
        } else {
            KUID other = (KUID)o;
            return type == other.type && Arrays.equals(id, other.id);
        }
    }
    
    /**
     * Returns the current KUID as hex String
     */
    public String toHexString() {
        return ArrayUtils.toHexString(id);
    }
    
    /**
     * Returns the current KUID as bin String
     */
    public String toBinString() {
        return ArrayUtils.toBinString(id);
    }
    
    /**
     * Returns the current KUID as BigInteger
     */
    public BigInteger toBigInteger() {
        // unsigned!        
        return new BigInteger(1, id);
    }
    
    /**
     * Returns the approximate log2. See BigInteger.bitLength()
     * for more info!
     */
    public int log() {
        return new BigInteger(1, id).bitLength();
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        switch(type) {
            case NODE_ID:
                buffer.append("NODE_ID: ");
                break;
            case VALUE_ID:
                buffer.append("VALUE_ID: ");
                break;
            case MESSAGE_ID:
                buffer.append("MESSAGE_ID: ");
                break;
            default:
                buffer.append("UNKNOWN_ID: ");
                break;
        }
        buffer.append(toHexString());
        return buffer.toString();
    }
    
    /**
     * Creates and returns a random Node ID. The SocketAddress
     * can be null and it's used to just add some extra randomness.
     */
    public static KUID createRandomNodeID(SocketAddress address) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            SecureRandom generator = SecureRandom.getInstance("SHA1PRNG");
            
            Properties props = System.getProperties();
            for(Enumeration e = props.keys(); e.hasMoreElements(); ) {
                String key = (String)e.nextElement();
                String value = (String)props.get(key);
                
                md.update(key.getBytes("UTF-8"));
                md.update(value.getBytes("UTF-8"));
            }
            
            byte[] random = new byte[generator.nextInt(256)];
            generator.nextBytes(random);
            md.update(random);
            
            long time = System.currentTimeMillis();
            md.update((byte)((time >> 56L) & 0xFFL));
            md.update((byte)((time >> 48L) & 0xFFL));
            md.update((byte)((time >> 40L) & 0xFFL));
            md.update((byte)((time >> 32L) & 0xFFL));
            md.update((byte)((time >> 24L) & 0xFFL));
            md.update((byte)((time >> 16L) & 0xFFL));
            md.update((byte)((time >>  8L) & 0xFFL));
            md.update((byte)((time       ) & 0xFFL));
            
            if (address instanceof InetSocketAddress) {
                InetSocketAddress addr = (InetSocketAddress)address;
                md.update(addr.getAddress().getAddress());
                
                int port = addr.getPort();
                md.update((byte)((port >>  8) & 0xFF));
                md.update((byte)((port      ) & 0xFF));
            }
            
            return createNodeID(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Creates and returns a Node ID from a byte array
     */
    public static KUID createNodeID(byte[] id) {
        return new KUID(NODE_ID, id);
    }
    
    /**
     * Creates and returns a Value ID from a byte array
     */
    public static KUID createValueID(byte[] id) {
        return new KUID(VALUE_ID, id);
    }
    
    /**
     * Creates and returns a random Message ID
     */
    public static KUID createRandomMessageID() {
        byte[] id = new byte[LENGTH/8];
        GENERATOR.nextBytes(id);
        return createMessageID(id);
    }
    
    /**
     * Creates and returns a Message ID from the byte array
     */
    public static KUID createMessageID(byte[] id) {
        return new KUID(MESSAGE_ID, id);
    }
    
    /**
     * Creates a random ID with the specified byte prefix
     * 
     * @param prefix the fixed prefix bytes
     * @return a random KUID starting with the given prefix
     */
    private static KUID createPrefxNodeID(byte[] prefix, int depth, byte[] random) {
        ++depth;
        int length = (int)(depth)/8;
        System.arraycopy(prefix,0,random,0,length);
        if((depth % 8) != 0) {
            int bitsToCopy = (depth) % 8;
            // Mask has the low-order (8-bits) bits set
            int mask = (1 << (8-bitsToCopy)) - 1;
            int prefixByte = prefix[length];
            int randByte   = random[length];
            random[length] = (byte) ((prefixByte & ~mask) | (randByte & mask));
        }
        return KUID.createNodeID(random);
    }
    
    /**
     * Creates a random ID with the specified byte prefix
     * 
     * @param prefix the fixed prefix bytes
     * @return a random KUID starting with the given prefix
     */
    public static KUID createPrefxNodeID(byte[] prefix, int depth) {
        byte[] random = new byte[20];
        GENERATOR.nextBytes(random);
        return createPrefxNodeID(prefix,depth,random);
    }
    
    /**
     * Creates and returns an Unknown ID
     */
    public static KUID createUnknownID() {
        byte[] id = new byte[LENGTH/8];
        GENERATOR.nextBytes(id);
        return createUnknownID(id);
    }
    
    /**
     * Creates and returns an Unknown ID from the byte array
     */
    public static KUID createUnknownID(byte[] id) {
        return new KUID(UNKNOWN_ID, id);
    }
}
