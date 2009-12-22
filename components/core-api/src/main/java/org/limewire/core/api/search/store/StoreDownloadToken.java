package org.limewire.core.api.search.store;

/**
 * Defines the result for an approval request to download a store file.
 */
public interface StoreDownloadToken {
    public enum Status {
        APPROVED, CONFIRM_REQ, LOGIN_REQ, FAILED
    }

    /**
     * Returns the status for the download approval request.
     */
    Status getStatus();
    
    /**
     * Returns the URL text for the approval web page.  May be null if no
     * further approval is required.
     */
    String getUrl();
}
