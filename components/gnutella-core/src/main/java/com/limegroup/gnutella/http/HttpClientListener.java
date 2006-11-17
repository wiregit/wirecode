package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.commons.httpclient.HttpMethod;

/**
 * Callback used by an HttpExecutor in order to inform about the status
 * of arbitrary HttpMethod requests.
 */
public interface HttpClientListener {
    
    /** Notification that the HttpMethod completed. */
	public void requestComplete(HttpMethod method);
    
    /** Notification that the HttpMethod failed. */
	public void requestFailed(HttpMethod method, IOException exc);
}
