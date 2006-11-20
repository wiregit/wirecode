package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.commons.httpclient.HttpMethod;

/**
 * Callback used by an HttpExecutor in order to inform about the status
 * of arbitrary HttpMethod requests.
 */
public interface HttpClientListener {
    
    /**
     * Notification that the HttpMethod completed.
     * Returns true if more requests should be processed, false otherwise.
     * (The return value only makes sense in the case that multiple methods
     *  are being handled by a single HttpClientListener.)
     */
	public boolean requestComplete(HttpMethod method);
    
    /**
     * Notification that the HttpMethod failed.
     * Returns true if more requests should be processed, false otherwise.
     * (The return value only makes sense in the case that multiple methods
     *  are being handled by a single HttpClientListener.)
     */
	public boolean requestFailed(HttpMethod method, IOException exc);
}
