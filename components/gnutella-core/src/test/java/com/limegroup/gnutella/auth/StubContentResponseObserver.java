package com.limegroup.gnutella.auth;

import org.limewire.io.URNImpl;

public class StubContentResponseObserver implements ContentResponseObserver {

    private URNImpl urn;
    private ContentResponseData response;
    
    public void handleResponse(URNImpl urn, ContentResponseData response) {
        this.urn = urn;
        this.response = response;
    }

    public ContentResponseData getResponse() {
        return response;
    }

    public URNImpl getUrn() {
        return urn;
    }

}
