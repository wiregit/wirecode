package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

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
	public boolean requestComplete(HttpUriRequest request, HttpResponse response);
    
    /**
     * Notification that the HttpMethod failed.
     * Returns true if more requests should be processed, false otherwise.
     * (The return value only makes sense in the case that multiple methods
     *  are being handled by a single HttpClientListener.)
     * TODO remove response from signature
     */
	public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc);
}
