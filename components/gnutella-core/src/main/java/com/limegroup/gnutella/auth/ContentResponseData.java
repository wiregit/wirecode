package com.limegroup.gnutella.auth;

import java.io.Serializable;

import com.limegroup.gnutella.messages.vendor.ContentResponse;

/** A response for content. */
public class ContentResponseData implements Serializable {
    private static final long serialVersionUID = -2625113217839178878L;
    
    private long created;
    
    private boolean ok;
    
    private String message;
    
    /** Constructs a new ContentResponseData with data from the given ContentResponse */
    public ContentResponseData(ContentResponse msg) {
        this(System.currentTimeMillis(), msg.isOK(), msg.getMessage());
    }
    
    /** Hook for tests to create Responses at different times. */
    private ContentResponseData(long now, boolean ok, String message) {
        this.created = now;
        this.ok = ok;
        this.message = message;
    }
    
    /** Returns if this is OK. */
    public boolean isOK() {
        return ok;
    }
    
    public String getMessage() {
        return message;
    }
    
    /** Returns the time this ContentResponseData was created. */
    public long getCreationTime() {
        return created;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("OK: ").append(isOK()).append("\n");
        buffer.append("Message: ").append(getMessage()).append("\n");
        return buffer.toString();
    }
}
