package com.limegroup.gnutella.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.limegroup.gnutella.ByteOrder;

/**
 * Utility class that supplies commonly used data sets that each
 * class should not have to create on its own.  These data sets
 * are immutable objects, so any class and any thread may access them
 * whenever they like.
 */
public final class DataUtils {
    
    /**
     * Ensure that this class cannot be constructed.
     */
    private DataUtils() {}
    
    /**
     * Constant empty byte array for any class to use -- immutable.
     */
    public static byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
    /**
     * An empty byte array length 1.
     */
    public static byte[] BYTE_ARRAY_ONE = new byte[1];
    
    /**
     * An empty byte array length 2.
     */
    public static byte[] BYTE_ARRAY_TWO = new byte[2];
    
    /**
     * An empty byte array length 3.
     */
    public static byte[] BYTE_ARRAY_THREE = new byte[3];
    
    static {
        BYTE_ARRAY_ONE[0] = 0;
        BYTE_ARRAY_TWO[0] = 0;
        BYTE_ARRAY_TWO[1] = 0;
        BYTE_ARRAY_THREE[0] = 0;
        BYTE_ARRAY_THREE[1] = 0;
        BYTE_ARRAY_THREE[2] = 0;
    }
    
    /**
     * Constant empty string array for any class to use -- immutable.
     */
    public static String[] EMPTY_STRING_ARRAY = new String[0];
        
    /**
     * An 16-length empty byte array, for GUIDs.
     */
    public static final byte[] EMPTY_GUID = new byte[16];
    
    /**
     * The amount of milliseconds in a week.
     */
    public static final long ONE_WEEK = 7 * 24 * 60 * 60 * 1000;
    
    /**
     * Determines whether or not the the child Set contains any elements
     * that are in the parent's set.
     */
    public static boolean containsAny(Collection parent, Collection children) {
        for(Iterator i = children.iterator(); i.hasNext(); )
            if(parent.contains(i.next()))
                return true;
        return false;
    }    
    
    /**
     * Utility function to write out the toString contents
     * of a URN.
     */
    public static String listSet(Set s) {
        StringBuffer sb = new StringBuffer();
        for(Iterator i = s.iterator(); i.hasNext();)
            sb.append(i.next().toString());
        return sb.toString();
    }

    /**
     * Prints out the contents of the input array as a hex string.
     */
    public static String toHexString(byte[] bytes) {
        StringBuffer buf=new StringBuffer();
        String str;
        int val;
        for (int i=0; i<bytes.length; i++) {
            //Treating each byte as an unsigned value ensures
            //that we don't str doesn't equal things like 0xFFFF...
            val = ByteOrder.ubyte2int(bytes[i]);
            str = Integer.toHexString(val);
            while ( str.length() < 2 )
            str = "0" + str;
            buf.append( str );
        }
        return buf.toString().toUpperCase();
    }
    
    /**
     * packs an array of numerical values into a bit vector, where each value is
     * represented with size bits.
     */
    public static byte[] bitPack(int [] values, int size) {
        int bitSize = values.length * size;
        int retSize = bitSize / 8;
        if (bitSize % 8 != 0)
            retSize++;
        byte []ret = new byte[retSize];
        
        BitSet tmp = new BitSet(bitSize);
        int offset = 0;
        int mask = 1 << size-1;
        for (int j = 0;j< values.length;j++) {
            int element = values[j];
            for (int i = 0; i < size; i++) {
                if (((element << i) & mask) == mask) 
                    tmp.set(offset);
                offset++;
            }
        }
        
        for (int i = 0;i < tmp.length();i++) {
            if (tmp.get(i)) 
                ret[i/8] |= (0x80 >>> (i % 8));
        }
        
        return ret;
    }
    
    /**
     * @param data an array of integer values, packed to bits.
     * @param offset where in the array to start unpacking
     * @param number the number of values to try and unpack
     * @param size the size of each value in bits
     * @return array of int values, unpacked.
     * @throws IOException if there isn't enough data in the source array.
     */
    public static int [] bitUnpack(byte [] data,int offset, int number, int size) 
    	throws IOException {
        int bitSize = number * size;
        int arraySize = bitSize / 8;
        if (bitSize % 8 != 0)
            arraySize++;
        if (data.length < offset + arraySize)
            throw new IOException("cannot unpack values");
        
        int []ret = new int[number];
        
        // if the size is 8, parse the fast way
        if (size == 8) {
            for (int i = 0;i < number;i++)
                ret[i] = data[offset+i] & 0xFF;
            return ret;
        }
        
        // otherwise, the slow way
        offset= offset * 8;// the offset here is in bits
        for (int i = 0; i < number; i++) {
            int element=0;
            for (int j = 0; j < size; j++) {
                int current = data[offset / 8];
                int extractedBit = current & (0x80 >>> (offset % 8));
                extractedBit >>= (7 - (offset % 8));
                element <<= 1;
                element |= extractedBit;
                offset++;
            }
            ret[i] = element;
        }
        
        return ret;
    }
}
