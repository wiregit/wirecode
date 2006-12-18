package com.limegroup.gnutella.auth;

import java.io.Serializable;
import java.util.NoSuchElementException;

import com.limegroup.gnutella.messages.vendor.ContentResponse;

/** A response for content. */
public class ContentResponseData implements Serializable {
    private static final long serialVersionUID = -2625113217839178878L;
    
    private long created;
    
    protected Authorization auth;
    
    private String message;
    
    public static enum Authorization {
    	UNKNOWN(0),
    	AUTHORIZED(1),
    	UNAUTHORIZED(2);
    	
    	private final int value;
    	private final static Authorization[] auths;
    	
    	static {
    		Authorization[] a = values();
    		auths = new Authorization[a.length];
    		for (int i = 0; i < a.length; i++) {
				int index = a[i].value % a.length;
				if (auths[index] != null) {
					throw new IllegalStateException("hash collision");
				}
				auths[index] = a[i];
			}
    	}
    	
    	private Authorization(int value) {
    		this.value = value;
    	}
    	
    	public int getValue() {
    		return value;
    	}
    	
    	public static Authorization valueOf(int value) {
    		int index = value % auths.length;
    		if (auths[index].value == value) {
    			return auths[index];
    		}
    		throw new NoSuchElementException("Not a valid Authorization value");
    	}
    }
    
    /** Constructs a new ContentResponseData with data from the given ContentResponse */
    public ContentResponseData(ContentResponse msg) {
        this(System.currentTimeMillis(), msg.getAuthorization(), msg.getMessage());
    }
    
    public ContentResponseData(Authorization auth, String message) {
    	this(System.currentTimeMillis(), auth, message);
    }
    
    /** Hook for tests to create Responses at different times. */
    ContentResponseData(long now, Authorization auth, String message) {
        this.created = now;
        this.auth = auth;
        this.message = message;
    }
    
    protected ContentResponseData() {
    	this.created = System.currentTimeMillis();
    }
    
    public Authorization getAuthorization() {
    	return auth;
    }
    
    public boolean isAuthorized() {
    	return auth != Authorization.UNAUTHORIZED;
    }
    
    public boolean isUnauthorized() {
    	return auth == Authorization.UNAUTHORIZED;
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
        buffer.append("Authorization: ").append(getAuthorization()).append("\n");
        buffer.append("Message: ").append(getMessage()).append("\n");
        return buffer.toString();
    }
}
