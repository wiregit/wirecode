package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.messages.vendor.ContentResponse;

/** A response for content. */
public class Response {
    
    private boolean ok;
    
    public Response(ContentResponse msg) {
        ok = msg.getOK();
    }
    
    public boolean isOK() {
        return ok;
    }
}
