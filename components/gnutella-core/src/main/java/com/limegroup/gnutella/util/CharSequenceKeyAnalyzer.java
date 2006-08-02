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
    
    public int bitIndex(CharSequence key,   int keyOff, int keyLength,
                        CharSequence found, int foundOff, int foundKeyLength) {
        boolean allNull = true;
        
        keyLength += keyOff;
        foundKeyLength += foundOff;
        int length = Math.max(keyLength, foundKeyLength);
        
        // TODO:
        // At each index (starting with the index of startAt),
        // get the XOR of each, (if it's startAt, then shift it accordingly),
        // then get the number of trailing (leading?) zeros, account back for
        // startAt, and return the result.
        
        for (int i = 0; i < length; i++) {
            boolean a = isBitSet(key, keyLength, i+keyOff);
            boolean b = isBitSet(found, foundKeyLength, i+foundOff);
            
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

    public boolean isPrefix(CharSequence prefix, int offset, int length, CharSequence key) {
        if(offset % 16 != 0 || length % 16 != 0)
            throw new IllegalArgumentException("Cannot determine prefix outside of character boundaries");
        String s1 = prefix.subSequence(offset / 16, length / 16).toString();
        String s2 = key.toString();
        return s2.startsWith(s1);
    }
}

