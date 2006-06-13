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
 
package com.limegroup.mojito;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.util.ArrayUtils;
import com.limegroup.mojito.util.PatriciaTrie.KeyCreator;


/**
 * KUID stands for Kademlia Unique Identifier and represents 
 * an 160bit value. You're welcome to proposal a better name 
 * which is suitable for Keys, NodeIDs and MessageIDs!
 * 
 * This class is immutable!
 * 
 * TODO: Maybe Key, NodeID and MessageID but 'Key' sucks!
 */
public class KUID implements Serializable, Comparable {
    
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
    
    /**
     * A random pad we're using to obfuscate the actual
     * QueryKey. Nodes must do a lookup to get the QK!
     */
    private static final byte[] RANDOM_PAD = new byte[4];
                                                
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
        
        GENERATOR.nextBytes(RANDOM_PAD);
    }
    
    protected final int type;
    private byte[] id;
    
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
     * Returns the identity and throws an AssertionError
     * if this isn't a Node ID.
     */
    public KUID assertNodeID() throws AssertionError {
        if (!isNodeID()) {
            throw new AssertionError(this + " is not a NODE_ID");
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
     * Returns the identity and throws an AssertionError
     * if this isn't a Value ID.
     */
    public KUID assertValueID() throws AssertionError {
        if (!isValueID()) {
            throw new AssertionError(this + " is not a VALUE_ID");
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
     * Returns the identity and throws an AssertionError
     * if this isn't a Message ID.
     */
    public KUID assertMessageID() throws AssertionError {
        if (!isMessageID()) {
            throw new AssertionError(this + " is not a MESSAGE_ID");
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
        for (int i = 0; i < id.length; i++){
            int dSelf = (id[i] ^ targetID.id[i]) & 0xFF;
            int dOther = (nodeID.id[i] ^ targetID.id[i]) & 0xFF;
            int diff = dOther - dSelf;
            
            if (diff > 0) {
                return true;
            } else if (diff < 0) {
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
    
    /**
     * Returns the raw bytes of the current KUID
     */
    public byte[] getBytes() {
        return getBytes(0, new byte[id.length], 0, id.length);
    }
    
    /**
     * Returns the raw bytes of the current KUID from the specified interval
     */
    public byte[] getBytes(int srcPos, byte[] dest, int destPos, int length) {
        System.arraycopy(id, srcPos, dest, destPos, length);
        return dest;
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    public int compareTo(Object o) {
        KUID other = (KUID)o;
        
        int d = 0;
        for(int i = 0; i < id.length; i++) {
            d = (id[i] & 0xFF) - (other.id[i] & 0xFF);
            if (d < 0) {
                return -1;
            } else if (d > 0) {
                return 1;
            }
        }
        
        return 0;
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
     * Converts the current KUID into a Node ID if it isn't already.
     */
    public KUID toNodeID() {
        return (isNodeID() ? this : new KUID(NODE_ID, id));
    }
    
    /**
     * Converts the current KUID into a Value ID if it isn't already.
     */
    public KUID toValueID() {
        return (isValueID() ? this : new KUID(VALUE_ID, id));
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
    public int log2() {
        return toBigInteger().bitLength();
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
    public static KUID createRandomNodeID() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            //SecureRandom generator = SecureRandom.getInstance("SHA1PRNG");
            
            /*Properties props = System.getProperties();
            for(Enumeration e = props.keys(); e.hasMoreElements(); ) {
                String key = (String)e.nextElement();
                String value = (String)props.get(key);
                
                md.update(key.getBytes("UTF-8"));
                md.update(value.getBytes("UTF-8"));
            }*/
            
            byte[] random = new byte[Math.max(1, GENERATOR.nextInt(256))];
            for(int i = Math.max(1, GENERATOR.nextInt(32)); i >= 0; i--) {
                GENERATOR.nextBytes(random);
                md.update(random);
            }
            
            long time = System.currentTimeMillis();
            md.update((byte)((time >> 56L) & 0xFFL));
            md.update((byte)((time >> 48L) & 0xFFL));
            md.update((byte)((time >> 40L) & 0xFFL));
            md.update((byte)((time >> 32L) & 0xFFL));
            md.update((byte)((time >> 24L) & 0xFFL));
            md.update((byte)((time >> 16L) & 0xFFL));
            md.update((byte)((time >>  8L) & 0xFFL));
            md.update((byte)((time       ) & 0xFFL));
            
            md.update(RANDOM_PAD);
            
            return createNodeID(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }/* catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }*/
    }
    
    /**
     * Creates and returns a Node ID from a byte array
     */
    public static KUID createNodeID(byte[] id) {
        return new KUID(NODE_ID, id);
    }
    
    private static byte[] getBytes(ByteBuffer data) {
        byte[] id = new byte[LENGTH/8];
        data.get(id);
        return id;
    }
    
    /**
     * Creates and returns a Node ID from the ByteBuffer (relative get)
     */
    public static KUID createNodeID(ByteBuffer data) {
        return new KUID(NODE_ID, getBytes(data));
    }
    
    /**
     * Creates and returns a Value ID from a byte array
     */
    public static KUID createValueID(byte[] id) {
        return new KUID(VALUE_ID, id);
    }
    
    /**
     * Creates and returns a Value ID from the ByteBuffer (relative get)
     */
    public static KUID createValueID(ByteBuffer data) {
        return new KUID(VALUE_ID, getBytes(data));
    }
    
    /**
     * Creates a random message ID.
     */
    public static KUID createRandomMessageID() {
        return createRandomMessageID(null);
    }
    
    /**
     * Creates a random message ID and piggy packs a QueryKey to it.
     */
    public static KUID createRandomMessageID(SocketAddress dest) {
        byte[] id = new byte[LENGTH/8];
        GENERATOR.nextBytes(id);
        
        if (dest instanceof InetSocketAddress) {
            byte[] queryKey = QueryKey.getQueryKey(dest).getBytes();
            
            // Obfuscate it with our random pad!
            for(int i = 0; i < RANDOM_PAD.length; i++) {
                queryKey[i] ^= RANDOM_PAD[i];
            }
            
            System.arraycopy(queryKey, 0, id, 0, queryKey.length);
        }
        
        return createMessageID(id);
    }
    
    /**
     * Extracts and returns the QueryKey from the KUID if the
     * KUID is a message ID and null if it's a KUID of a different
     * type.
     */
    private QueryKey getQueryKey() {
        if (!isMessageID()) {
            return null;
        }
        
        byte[] queryKey = new byte[4];
        System.arraycopy(id, 0, queryKey, 0, queryKey.length);
        
        // De-Obfuscate it with our random pad!
        for(int i = 0; i < RANDOM_PAD.length; i++) {
            queryKey[i] ^= RANDOM_PAD[i];
        }
        
        return QueryKey.getQueryKey(queryKey, true);
    }
    
    /**
     * Returns wheather or not we're the originator of the message ID.
     */
    public boolean verifyQueryKey(SocketAddress src) {
        if (!isMessageID() || !(src instanceof InetSocketAddress)) {
            return false;
        }
        
        return getQueryKey().isFor(src);
    }
    
    /**
     * Creates and returns a Message ID from the byte array
     */
    public static KUID createMessageID(byte[] id) {
        return new KUID(MESSAGE_ID, id);
    }
    
    /**
     * Creates and returns a Message ID from the ByteBuffer (relative get)
     */
    public static KUID createMessageID(ByteBuffer data) {
        return new KUID(MESSAGE_ID, getBytes(data));
    }
    
    /**
     * Creates a random ID with the specified byte prefix
     * 
     * @param prefix the fixed prefix bytes
     * @return a random KUID starting with the given prefix
     */
    private static KUID createPrefxNodeID(byte[] prefix, int depth, byte[] random) {
        depth++;
        int length = (int)depth/8;
        System.arraycopy(prefix, 0, random, 0, length);
        
        int bitsToCopy = depth % 8;
        if (bitsToCopy != 0) {
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
