package com.limegroup.gnutella.messages;

/**
 * Allows multiple GGEP blocks to be parsed, storing
 * the 'secure GGEP' block separately.  Can store
 * the position where the secure block began & ended,
 * so that the rest of the data can be properly verified.
 */
public class GGEPParser {
    
    private final GGEP normal;
    private final GGEP secure;
    private final int secureStart;
    private final int secureEnd;
    
    private GGEPParser() {
        this(null, null, -1, -1);
    }
    
    private GGEPParser(GGEP normal, GGEP secure, int start, int end) {
        this.normal = normal;
        this.secure = secure;
        this.secureStart = start;
        this.secureEnd = end;
    }
    
    /**
     * Scans through the data, starting at idx, looking for the first
     * spot that has GGEP_PREFIX_MAGIC_NUMBER, and parses GGEP blocks
     * from there.
     */
    public static GGEPParser scanForGGEPs(byte[] data, int idx) {
        // Find the beginning of the GGEP block.
        for (; 
             idx < data.length &&
             data[idx] != GGEP.GGEP_PREFIX_MAGIC_NUMBER;
             idx++);
        
        if(idx >= data.length)
            return new GGEPParser(); // nothing to parse.
            
        int[] storage = new int[1];
        GGEP normal = null;
        GGEP secure = null;
        int secureStart = -1;
        int secureEnd = -1;
            
        try {
            while(secure == null && idx < data.length) {
                GGEP ggep = new GGEP(data, idx, storage);
                if(ggep.hasKey(GGEP.GGEP_HEADER_SECURE_BLOCK)) {
                    secure = ggep;
                    secureStart = idx;
                    secureEnd = storage[0];
                    break;
                } else {
                    if(normal == null)
                        normal = ggep;
                    else
                        normal.merge(ggep);
                    idx = storage[0] + 1;
                    storage[0] = -1;
                }
            }
        } catch (BadGGEPBlockException ignored) {
        }
        
        return new GGEPParser(normal, secure, secureStart, secureEnd);
    }
    
    
    public GGEP getNormalGGEP() {
        return normal;
    }
    
    public GGEP getSecureGGEP() {
        return secure;
    }
    
    public int getSecureStartIndex() {
        return secureStart;
    }
    
    public int getSecureEndIndex() {
        return secureEnd;
    }

}
