/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
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
 * TODO: Maybe Key, NodeID and MessageID but 'Key' sucks!
 * 
 * @author Roger Kapsi
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
    
    public static final int UNKNOWN_ID = 0x00;
    public static final int NODE_ID = 0x01;
    public static final int VALUE_ID = 0x02;
    public static final int MESSAGE_ID = 0x04;
    
    public static final KUID MIN_UNKNOWN_ID;
    public static final KUID MAX_UNKNOWN_ID;
    
    public static final KUID MIN_NODE_ID;
    public static final KUID MAX_NODE_ID;
    
    public static final KUID MIN_VALUE_ID;
    public static final KUID MAX_VALUE_ID;
    
    public static final KUID MIN_MESSAGE_ID;
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
    
    private final int hashCode;
    
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
    
    public KUID assertNodeID() throws RuntimeException {
        if (!isNodeID()) {
            throw new RuntimeException(this + " is not a NODE_ID");
        }
        return this;
    }
    
    public boolean isNodeID() {
        return type == NODE_ID;
    }
    
    public KUID assertValueID() throws RuntimeException {
        if (!isValueID()) {
            throw new RuntimeException(this + " is not a VALUE_ID");
        }
        return this;
    }
    
    public boolean isValueID() {
        return type == VALUE_ID;
    }
    
    public KUID assertMessageID() throws RuntimeException {
        if (!isMessageID()) {
            throw new RuntimeException(this + " is not a MESSAGE_ID");
        }
        return this;
    }
    
    public boolean isMessageID() {
        return type == MESSAGE_ID;
    }
    
    public boolean isUnknownID() {
        return type == UNKNOWN_ID;
    }
    
    public boolean isBitSet(int bitIndex) {
        int index = (int) (bitIndex / BITS.length);
        int bit = (int) (bitIndex - index * BITS.length);
        return (id[index] & BITS[bit]) != 0;
    }
    
    public KUID set(int bit) {
        return set(bit, true);
    }
    
    public KUID unset(int bit) {
        return set(bit, false);
    }
    
    private KUID set(int bitIndex, boolean set) {
        int index = (int) (bitIndex / BITS.length);
        int bit = (int) (bitIndex - index * BITS.length);
        
        byte[] id = getBytes();
        
        if (set) {
            id[index] |= BITS[bit];
        } else {
            id[index] &= ~BITS[bit];
        }
        
        return new KUID(type, id);
    }
    
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
    
    public KUID xor(KUID nodeId) {
        int sum = 0;
        byte[] result = new byte[id.length];
        for(int i = 0; i < result.length; i++) {
            int xor = (id[i] ^ nodeId.id[i]) & 0xFF;
            
            result[i] = (byte)xor;
            sum += xor;
        }
        
        int t = (type == nodeId.type) ? type : UNKNOWN_ID;
        
        if (sum == 0) {
            switch(type) {
                case NODE_ID:
                    return MIN_NODE_ID;
                case VALUE_ID:
                    return MIN_VALUE_ID;
                case MESSAGE_ID:
                    return MIN_MESSAGE_ID;
                default:
                    return MIN_UNKNOWN_ID;
            }
        }
        
        return new KUID(t, result);
    }
    
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
    
    public int getType() {
        return type;
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    public byte[] getBytes() {
        byte[] clone = new byte[id.length];
        System.arraycopy(id, 0, clone, 0, id.length);
        return clone;
    }
    
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
    
    public String toHexString() {
        return ArrayUtils.toHexString(id);
    }
    
    public String toBinString() {
        return ArrayUtils.toBinString(id);
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
    
    public static KUID createNodeID(byte[] id) {
        return new KUID(NODE_ID, id);
    }
    
    public static KUID createValueID(byte[] id) {
        return new KUID(VALUE_ID, id);
    }
    
    public static KUID createRandomMessageID() {
        byte[] id = new byte[LENGTH/8];
        GENERATOR.nextBytes(id);
        return createMessageID(id);
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
    
    
    public static KUID createMessageID(byte[] id) {
        return new KUID(MESSAGE_ID, id);
    }
    
    public static KUID createUnknownID() {
        byte[] id = new byte[LENGTH/8];
        GENERATOR.nextBytes(id);
        return createUnknownID(id);
    }
    
    public static KUID createUnknownID(byte[] id) {
        return new KUID(UNKNOWN_ID, id);
    }
    
    public BigInteger toBigInteger() {
        // unsigned!
        byte[] num = new byte[1 + id.length];
        System.arraycopy(id, 0, num, 1, id.length);
        return new BigInteger(num);
    }
    
    /**
     * Returns the approximate log2. See BigInteger.bitLength()
     * for more info!
     */
    public int log() {
        return new BigInteger(id).bitLength();
    }
}
