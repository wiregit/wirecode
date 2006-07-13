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
 
package com.limegroup.mojito;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.util.ArrayUtils;
import com.limegroup.mojito.util.PatriciaTrie.KeyCreator;


/**
 * KUID stands for Kademlia Unique Identifier and represents 
 * an 160bit value.
 * 
 * This class is immutable!
 */
public class KUID implements Comparable<KUID>, Serializable {
    
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
    
    /** Types of KUIDs that exist */
    public static enum Type {
        UNKNOWN_ID,
        NODE_ID,
        VALUE_ID,
        MESSAGE_ID;
    }
    
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
        
        MIN_UNKNOWN_ID = new KUID(Type.UNKNOWN_ID, min);
        MAX_UNKNOWN_ID = new KUID(Type.UNKNOWN_ID, max);
        
        MIN_NODE_ID = new KUID(Type.NODE_ID, min);
        MAX_NODE_ID = new KUID(Type.NODE_ID, max);
        
        MIN_VALUE_ID = new KUID(Type.VALUE_ID, min);
        MAX_VALUE_ID = new KUID(Type.VALUE_ID, max);
        
        MIN_MESSAGE_ID = new KUID(Type.MESSAGE_ID, min);
        MAX_MESSAGE_ID = new KUID(Type.MESSAGE_ID, max);
        
        GENERATOR.nextBytes(RANDOM_PAD);
    }
    
    private Type type;
    private byte[] id;
    
    private int hashCode = -1;
    
    protected KUID(Type type, byte[] id) {
        if (id == null) {
            throw new NullPointerException("ID is null");
        }
        
        if ((id.length * 8) != LENGTH) {
            throw new IllegalArgumentException("ID must be " + LENGTH + " bits long");
        }
        
        this.type = type;
        this.id = id;
    }
    
    /**
     * 
     * @param out
     * @throws IOException
     */
    public void write(OutputStream out) throws IOException {
        out.write(id, 0, id.length);
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
        return type == Type.NODE_ID;
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
        return type == Type.VALUE_ID;
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
        return type == Type.MESSAGE_ID;
    }
    
    /**
     * Returns true if this is a Unknown ID
     */
    public boolean isUnknownID() {
        return type == Type.UNKNOWN_ID;
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
        
        Type t = (type == nodeId.type) ? type : Type.UNKNOWN_ID;
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
     * Compares this KUID with with targetId and nodeId with targetId
     * and returns true if this KUID is nearer to targetId than nodeId
     * is.
     * 
     * @param nodeId the KUID to compare to
     * @param targetId the target ID
     * @return true if this KUID is nearer to targetID, false otherwise
     */
    public boolean isNearer(KUID nodeId, KUID targetId) {
        for (int i = 0; i < id.length; i++){
            int dSelf = (id[i] ^ targetId.id[i]) & 0xFF;
            int dOther = (nodeId.id[i] ^ targetId.id[i]) & 0xFF;
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
    public Type getType() {
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
        if (hashCode == -1) {
            hashCode = Arrays.hashCode(id);
            if (hashCode == -1) {
                hashCode = 0;
            }
        }
        return hashCode;
    }
    
    public int compareTo(KUID o) {
        int d = 0;
        for(int i = 0; i < id.length; i++) {
            d = (id[i] & 0xFF) - (o.id[i] & 0xFF);
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
        return (isNodeID() ? this : new KUID(Type.NODE_ID, id));
    }
    
    /**
     * Converts the current KUID into a Value ID if it isn't already.
     */
    public KUID toValueID() {
        return (isValueID() ? this : new KUID(Type.VALUE_ID, id));
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
        return type + ": " + toHexString();
    }
    
    /**
     * Creates and returns a random Node ID that is hopefully
     * globally unique.
     */
    public static KUID createRandomNodeID() {
        
        /*
         * Random Numbers.
         */
        MessageDigestInput randomNumbers = new MessageDigestInput() {
            public void update(MessageDigest md) {
                byte[] random = new byte[1024];
                GENERATOR.nextBytes(random);
                md.update(random);
            }
        };
        
        /*
         * System Properties. Many of them are not unique but
         * properties like user.name, user.home or os.arch will
         * add some randomness.
         */
        MessageDigestInput properties = new MessageDigestInput() {
            public void update(MessageDigest md) {
                Properties props = System.getProperties();
                try {
                    for (Entry entry : props.entrySet()) {
                        String key = (String)entry.getKey();
                        String value = (String)entry.getValue();
                        
                        md.update(key.getBytes("UTF-8"));
                        md.update(value.getBytes("UTF-8"));
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        
        /*
         * System time in millis (GMT). Many computer clocks
         * are off. Should be a good source for randomness.
         */
        MessageDigestInput millis = new MessageDigestInput() {
            public void update(MessageDigest md) {
                long millis = System.currentTimeMillis();
                md.update((byte)((millis >> 56L) & 0xFFL));
                md.update((byte)((millis >> 48L) & 0xFFL));
                md.update((byte)((millis >> 40L) & 0xFFL));
                md.update((byte)((millis >> 32L) & 0xFFL));
                md.update((byte)((millis >> 24L) & 0xFFL));
                md.update((byte)((millis >> 16L) & 0xFFL));
                md.update((byte)((millis >>  8L) & 0xFFL));
                md.update((byte)((millis       ) & 0xFFL));
            }
        };
        
        /*
         * Our obfuscation pad. 4 random bytes!
         */
        MessageDigestInput randomPad = new MessageDigestInput() {
            public void update(MessageDigest md) {
                md.update(RANDOM_PAD);
            }
        };
        
        /*
         * VM/machine dependent pseudo time.
         */
        MessageDigestInput nanos = new MessageDigestInput() {
            public void update(MessageDigest md) {
                long nanos = System.nanoTime();
                md.update((byte)((nanos >> 56L) & 0xFFL));
                md.update((byte)((nanos >> 48L) & 0xFFL));
                md.update((byte)((nanos >> 40L) & 0xFFL));
                md.update((byte)((nanos >> 32L) & 0xFFL));
                md.update((byte)((nanos >> 24L) & 0xFFL));
                md.update((byte)((nanos >> 16L) & 0xFFL));
                md.update((byte)((nanos >>  8L) & 0xFFL));
                md.update((byte)((nanos       ) & 0xFFL));
            }
        };
        
        /*
         * Sort the MessageDigestInput(s) by their random
         * index (i.e. shuffle the array).
         */
        MessageDigestInput[] input = { 
                properties, 
                randomNumbers, 
                millis, 
                randomPad, 
                nanos
        };
        
        Arrays.sort(input);
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            
            // Get the SHA1...
            for(MessageDigestInput mdi : input) {
                mdi.update(md);
                
                // Hash also the identity hash code
                int hashCode = System.identityHashCode(mdi);
                md.update((byte)((hashCode >> 24) & 0xFFL));
                md.update((byte)((hashCode >> 16) & 0xFFL));
                md.update((byte)((hashCode >>  8) & 0xFFL));
                md.update((byte)((hashCode      ) & 0xFFL));
                
                // and the random index
                md.update((byte)((mdi.rnd >> 24) & 0xFFL));
                md.update((byte)((mdi.rnd >> 16) & 0xFFL));
                md.update((byte)((mdi.rnd >>  8) & 0xFFL));
                md.update((byte)((mdi.rnd      ) & 0xFFL));
            }
            
            return createNodeID(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * See KUID.createRandomNodeID()
     */
    private abstract static class MessageDigestInput 
            implements Comparable<MessageDigestInput> {
        
        private int rnd = GENERATOR.nextInt();
        
        public abstract void update(MessageDigest md);
        
        public int compareTo(MessageDigestInput o) {
            return rnd - o.rnd;
        }
    }
    
    /**
     * Creates and returns a Node ID from a byte array
     */
    public static KUID createNodeID(byte[] id) {
        return new KUID(Type.NODE_ID, id);
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
        return new KUID(Type.NODE_ID, getBytes(data));
    }
    
    /**
     * Creates and returns a Value ID from a byte array
     */
    public static KUID createValueID(byte[] id) {
        return new KUID(Type.VALUE_ID, id);
    }
    
    /**
     * Creates and returns a Value ID from the ByteBuffer (relative get)
     */
    public static KUID createValueID(ByteBuffer data) {
        return new KUID(Type.VALUE_ID, getBytes(data));
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
        return new KUID(Type.MESSAGE_ID, id);
    }
    
    /**
     * Creates and returns a Message ID from the ByteBuffer (relative get)
     */
    public static KUID createMessageID(ByteBuffer data) {
        return new KUID(Type.MESSAGE_ID, getBytes(data));
    }
    
    /**
     * Creates a random ID with the specified byte prefix
     * 
     * @param prefix the fixed prefix bytes
     * @return a random KUID starting with the given prefix
     */
    public static KUID createPrefxNodeID(KUID prefix, int depth) {
        byte[] random = new byte[20];
        GENERATOR.nextBytes(random);
        return createPrefxNodeID(prefix, depth, random);
    }
    
    /**
     * Creates a random ID with the specified byte prefix
     * 
     * @param prefix the fixed prefix bytes
     * @return a random KUID starting with the given prefix
     */
    private static KUID createPrefxNodeID(KUID prefix, int depth, byte[] random) {
        depth++;
        int length = (int)depth/8;
        System.arraycopy(prefix.id, 0, random, 0, length);
        
        int bitsToCopy = depth % 8;
        if (bitsToCopy != 0) {
            // Mask has the low-order (8-bits) bits set
            int mask = (1 << (8-bitsToCopy)) - 1;
            int prefixByte = prefix.id[length];
            int randByte   = random[length];
            random[length] = (byte) ((prefixByte & ~mask) | (randByte & mask));
        }
        
        return KUID.createNodeID(random);
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
        return new KUID(Type.UNKNOWN_ID, id);
    }
}
