package org.limewire.mojito.message;

/**
 * An interface for <tt>STORE</tt> response {@link Message}s.
 */
public interface StoreResponse extends ResponseMessage {

    /**
     * Returns the {@link StoreStatusCode}s for whether or not
     * the store operation was a success.
     */
    public StoreStatusCode[] getStoreStatusCodes();
}
