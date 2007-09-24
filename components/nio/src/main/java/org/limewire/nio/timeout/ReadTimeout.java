package org.limewire.nio.timeout;

/**
 * Defines the interface to get the read timeout value in milliseconds. 
 * Returning 0 implies the read timeout option is disabled (i.e., timeout of 
 * infinity). Returning a negative value implies an error.
 */
public interface ReadTimeout {
    public long getReadTimeout();
}
