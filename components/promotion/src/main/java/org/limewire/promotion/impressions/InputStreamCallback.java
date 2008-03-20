package org.limewire.promotion.impressions;

import java.io.InputStream;

/**
 * Processes a {@link InputStream}; usually as an asynchronous call.
 */
public interface InputStreamCallback {

    /**
     * Performs some work on <code>in</code>.
     * 
     * @param in input stream on which we do some work
     */
    void process(InputStream in);
}
