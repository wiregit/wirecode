package org.limewire.promotion.impressions;

/**
 * Processes a <code>byte</code> array; usually as an asynchronous call.
 */
public interface ByteArrayCallback {

    /**
     * Performs some work on <code>bytes</code>.
     * 
     * @param bytes array on which we do some work
     */
    void process(byte[] bytes);
}
