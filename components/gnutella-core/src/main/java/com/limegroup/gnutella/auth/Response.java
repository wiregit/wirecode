package com.limegroup.gnutella.auth;

import java.io.Serializable;

import com.limegroup.gnutella.messages.vendor.ContentResponse;

/** A response for content. */
public class Response implements Serializable {
    private static final long serialVersionUID = -2625113217839178878L;
    
    private long created;
    private boolean ok;
    
    /** Constructs a new Response with data from the given ContentResponse */
    public Response(ContentResponse msg) {
        this(System.currentTimeMillis(), msg.getOK());
    }
    
    /** Hook for tests to create Responses at different times. */
    private Response(long now, boolean ok) {
        this.created = now;
        this.ok = ok;
    }
    
    /** Returns if this is OK. */
    public boolean isOK() {
        return ok;
    }
    
    /** Returns the time this Response was created. */
    public long getCreationTime() {
        return created;
    }
}
