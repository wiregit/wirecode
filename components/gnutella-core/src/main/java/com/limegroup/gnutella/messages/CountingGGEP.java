package com.limegroup.gnutella.messages;

/**
 * A ggep that keeps an estimate of how much space it will take
 * when serialized.
 */
public class CountingGGEP extends GGEP {
    private static final int BLOCK_OVERHEAD = 1; // the magic byte
    private static final int KEY_OVERHEAD = 1; // overhead for the header
    private static final int VALUE_OVERHEAD = 3; // overhead for the size
    
    private int _counter = BLOCK_OVERHEAD;
    
    
    public void put(String key, byte[] value) throws IllegalArgumentException {
        super.put(key, value);
        _counter += KEY_OVERHEAD+VALUE_OVERHEAD+key.length()+value.length;
    }
    
    public void put(String key) throws IllegalArgumentException {
        super.put(key);
        _counter += KEY_OVERHEAD + key.length();
    }
    
    public void putAndCompress(String key, byte[] value)
            throws IllegalArgumentException {
        super.putAndCompress(key, value);
        value = get(key);
        _counter += KEY_OVERHEAD+VALUE_OVERHEAD+key.length()+value.length;
    }
    
    public int getEstimatedSize() {
        return _counter;
    }
    
    // Delegate all constructors
    public CountingGGEP() {
        super();
    }
    
    public CountingGGEP(boolean notNeedCOBS) {
        super(notNeedCOBS);
    }
    
    public CountingGGEP(byte[] data, int offset) throws BadGGEPBlockException {
        super(data, offset);
    }
    
    public CountingGGEP(byte[] messageBytes, int beginOffset, int[] endOffset)
            throws BadGGEPBlockException {
        super(messageBytes, beginOffset, endOffset);
        if (endOffset != null)
            _counter = endOffset[0] - beginOffset;
    }
}
