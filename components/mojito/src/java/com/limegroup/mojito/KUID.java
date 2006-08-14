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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;

import com.limegroup.gnutella.util.PatriciaTrie.KeyAnalyzer;
import com.limegroup.mojito.util.ArrayUtils;


/**
 * KUID stands for Kademlia Unique Identifier and represents 
 * an 160bit value.
 * 
 * This class is immutable!
 */
public class KUID implements Comparable<KUID>, Serializable {
    
    private static final long serialVersionUID = 633717248208386374L;
    
    private static final Random GENERATOR = new Random();
    
    public static final int LENGTH = 20;
    
    public static final int LENGTH_IN_BITS = LENGTH * 8; // 160 bit
    
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
    
    /** All 160 bits are 0 */
    public static final KUID MINIMUM;
    
    /** All 160 bits are 1 */
    public static final KUID MAXIMUM;
                                           
    static {
        byte[] min = new byte[LENGTH];
        
        byte[] max = new byte[LENGTH];
        Arrays.fill(max, (byte)0xFF);
        
        MINIMUM = new KUID(min);
        MAXIMUM = new KUID(max);
    }
    
    /** The id */
    private byte[] id;
    
    /** Lazyly initialized hashCode */
    private volatile int hashCode = -1;
    
    protected KUID(byte[] id) {
        if (id == null) {
            throw new NullPointerException("ID is null");
        }
        
        if (id.length != LENGTH) {
            throw new IllegalArgumentException("ID must be " + LENGTH + " bytes long");
        }
        
        this.id = id;
    }
    
    /**
     * Writes the ID to the OutputStream
     */
    public void write(OutputStream out) throws IOException {
        out.write(id, 0, id.length);
    }
    
    /**
     * Returns whether or not the 'bitIndex' th bit is set
     */
    public boolean isBitSet(int bitIndex) {
        // Take advantage of rounding errors!
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
    
    /**
     * Sets or unsets the 'bitIndex' th bit
     */
    private KUID set(int bitIndex, boolean set) {
        // Take advantage of rounding errors!
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
            return new KUID(id);
        } else {
            return this;
        }
    }
    
    /**
     * Returns the number of bits that are 1
     */
    public int bits() {
        int bits = 0;
        for(int i = 0; i < LENGTH_IN_BITS; i++) {
            if (isBitSet(i)) {
                bits++;
            }
        }
        return bits;
    }
    
    /**
     * Returns the first bit that differs in this KUID
     * and the given KUID or KeyAnalyzer.NULL_BIT_KEY
     * if all 160 bits are zero or KeyAnalyzer.EQUAL_BIT_KEY
     * if both KUIDs are equal
     */
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
            return KeyAnalyzer.NULL_BIT_KEY;
        }
        
        if (bitIndex == LENGTH_IN_BITS) {
            return KeyAnalyzer.EQUAL_BIT_KEY;
        }
        
        return bitIndex;
    }
    
    /**
     * Returns the xor distance between the current and given KUID.
     */
    public KUID xor(KUID nodeId) {
        byte[] result = new byte[id.length];
        for(int i = 0; i < result.length; i++) {
            result[i] = (byte)(id[i] ^ nodeId.id[i]);
        }
        
        return new KUID(result);
    }
    
    /**
     * Inverts all bits of the current KUID
     */
    public KUID invert() {
        byte[] result = new byte[id.length];
        for(int i = 0; i < result.length; i++) {
            result[i] = (byte)~id[i];
        }
        return new KUID(result);
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
     * Returns the raw bytes of the current KUID. The
     * returned byte[] array is a copy and modifications
     * are not reflected to this KUID
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
     * Returns whether or not both KUIDs are equal
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } if (!(o instanceof KUID)) {
            return false;
        } else {
            return Arrays.equals(id, ((KUID)o).id);
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
        return new BigInteger(1 /* unsigned! */, id);
    }
    
    /**
     * Returns the approximate log2. See BigInteger.bitLength()
     * for more info!
     */
    public int log2() {
        return toBigInteger().bitLength();
    }
    
    public String toString() {
        return toHexString();
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
            
            return create(md.digest());
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
     * Creates and returns a KUID from a byte array
     */
    public static KUID create(byte[] id) {
        return new KUID(id);
    }
    
    /**
     * Creates and returns a KUID from a hex encoded String
     */
    public static KUID create(String id) {
        return create(ArrayUtils.parseHexString(id));
    }
    
    /**
     * Creates a random ID with the specified byte prefix
     * 
     * @param prefix the fixed prefix bytes
     * @return a random KUID starting with the given prefix
     */
    public static KUID createPrefxNodeID(KUID prefix, int depth) {
        byte[] random = new byte[LENGTH];
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
        
        return KUID.create(random);
    }
    
    /**
     * The default KeyAnalyer for KUIDs
     */
    public static final KeyAnalyzer<KUID> KEY_ANALYZER = new KUIDKeyCreator();
    
    /**
     * A PATRICIA Trie KeyCreator for KUIDs
     */
    private static class KUIDKeyCreator implements KeyAnalyzer<KUID> {
        
        private static final long serialVersionUID = 6412279289438108492L;

        public int bitIndex(KUID key, int keyStart, int keyLength, KUID found, int foundStart, int foundLength) {
            if (found == null) {
                found = KUID.MINIMUM;
            }
            
            return key.bitIndex(found);
        }

        public int bitsPerElement() {
            return 1;
        }

        public boolean isBitSet(KUID key, int keyLength, int bitIndex) {
            return key.isBitSet(bitIndex);
        }

        public boolean isPrefix(KUID prefix, int offset, int length, KUID key) {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                if (prefix.isBitSet(i) != key.isBitSet(i)) {
                    return false;
                }
            }
            
            return true;
        }

        public int length(KUID key) {
            return KUID.LENGTH;
        }

        public int compare(KUID o1, KUID o2) {
            return o1.compareTo(o2);
        }
    }
}
