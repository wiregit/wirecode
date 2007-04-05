package com.limegroup.gnutella.util;

import com.limegroup.gnutella.util.PatriciaTrie.KeyAnalyzer;


public class CharSequenceKeyAnalyzer implements com.limegroup.gnutella.util.PatriciaTrie.KeyAnalyzer {
    
    private static final long serialVersionUID = -7032449491269434877L;
    
    private static final int[] BITS = createIntBitMask(16);
    
    public static final int[] createIntBitMask(int bitCount) {
        int[] bits = new int[bitCount];
        for(int i = 0; i < bitCount; i++) {
            bits[i] = 1 << (bitCount - i - 1);
        }
        return bits;
    }
    
    public int length(Object key) {
        return (key != null ? ((CharSequence)key).length() * 16 : 0);
    }
    
    public int bitIndex(Object key,   int keyOff, int keyLength,
                        Object found, int foundOff, int foundKeyLength) {
        boolean allNull = true;
        
        if(keyOff % 16 != 0 || foundOff % 16 != 0 ||
           keyLength % 16 != 0 || foundKeyLength % 16 != 0)
            throw new IllegalArgumentException("offsets & lengths must be at character boundaries");
        
        int off1 = keyOff / 16;
        int off2 = foundOff / 16;
        int len1 = keyLength / 16 + off1;
        int len2 = foundKeyLength / 16 + off2;
        int length = Math.max(len1, len2);
        
        // Look at each character, and if they're different
        // then figure out which bit makes the difference
        // and return it.
        char k = 0, f = 0;
        for(int i = 0; i < length; i++) {
            int kOff = i + off1;
            int fOff = i + off2;
            
            if(kOff >= len1)
                k = 0;
            else
                k = ((CharSequence)key).charAt(kOff);
            
            if(found == null || fOff >= len2)
                f = 0;
            else
                f = ((CharSequence)found).charAt(fOff);
            
            if(k != f) {
               int x = k ^ f;
               return i * 16 + (Integer.numberOfLeadingZeros(x) - 16);
            }
            
            if(k != 0)
                allNull = false;
            
        }
        
        if (allNull) {
            return KeyAnalyzer.NULL_BIT_KEY;
        }
        
        return KeyAnalyzer.EQUAL_BIT_KEY;
    }
    
    public boolean isBitSet(Object key, int keyLength, int bitIndex) {
        if (key == null || bitIndex >= keyLength) {
            return false;
        }
        
        int index = bitIndex / BITS.length;
        int bit = bitIndex - index * BITS.length;
        return (((CharSequence)key).charAt(index) & BITS[bit]) != 0;
    }

    public int compare(Object o1, Object o2) {
        return o1.toString().compareTo(o2.toString());
    }

    public int bitsPerElement() {
        return 16;
    }

    public boolean isPrefix(Object prefix, int offset, int length, Object key) {
        if(offset % 16 != 0 || length % 16 != 0)
            throw new IllegalArgumentException("Cannot determine prefix outside of character boundaries");
        String s1 = ((CharSequence)prefix).subSequence(offset / 16, length / 16).toString();
        String s2 = key.toString();
        return s2.startsWith(s1);
    }
}
