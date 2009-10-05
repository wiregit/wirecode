package org.limewire.core.impl.search.store;

import org.limewire.core.api.search.store.StoreDownloadToken;

/**
 * Implementation of StoreDownloadToken for the mock core.
 */
class MockStoreDownloadToken implements StoreDownloadToken {

    private final Status status;
    private final String url;
    
    /**
     * Constructs a MockStoreDownloadToken with the specified attributes.
     */
    public MockStoreDownloadToken(Status status, String url) {
        this.status = status;
        this.url = url;
    }
    
    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
