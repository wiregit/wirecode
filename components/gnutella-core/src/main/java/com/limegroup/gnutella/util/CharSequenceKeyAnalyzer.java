package com.limegroup.gnutella.util;

import com.limegroup.gnutella.util.PatriciaTrie.KeyAnalyzer;

public class CharSequenceKeyAnalyzer implements KeyAnalyzer<CharSequence> {
    
    private static final long serialVersionUID = -7032449491269434877L;
    
    private static final int[] BITS = createIntBitMask(16);
    
    public static final int[] createIntBitMask(int bitCount) {
        int[] bits = new int[bitCount];
        for(int i = 0; i < bitCount; i++) {
            bits[i] = 1 << (bitCount - i - 1);
        }
        return bits;
    }
    
    public int length(CharSequence key) {
        return (key != null ? key.length() * 16 : 0);
    }
    
    public int bitIndex(int startAt, CharSequence key, CharSequence found) {
        boolean allNull = true;
        
        int keyLength = length(key);
        int foundKeyLength = length(found);
        int length = Math.max(keyLength, foundKeyLength);
        
        // At each index (starting with the index of startAt),
        // get the XOR of each, (if it's startAt, then shift it accordingly),
        // then get the number of trailing (leading?) zeros, account back for
        // startAt, and return the result.
        
        for (int i = startAt; i < length; i++) {
            boolean a = isBitSet(key, keyLength, i);
            boolean b = isBitSet(found, foundKeyLength, i);
            
            if (a != b) {
                return i;
            }
            
            if (a) {
                allNull = false;
            }
        }
        
        if (allNull) {
            return KeyAnalyzer.NULL_BIT_KEY;
        }
        
        return KeyAnalyzer.EQUAL_BIT_KEY;
    }
    
    public boolean isBitSet(CharSequence key, int keyLength, int bitIndex) {
        if (key == null || bitIndex >= keyLength) {
            return false;
        }
        
        int index = bitIndex / BITS.length;
        int bit = bitIndex - index * BITS.length;
        return (key.charAt(index) & BITS[bit]) != 0;
    }

    public int compare(CharSequence o1, CharSequence o2) {
        return o1.toString().compareTo(o2.toString());
    }

    public int bitsPerElement() {
        return 16;
    }
}

