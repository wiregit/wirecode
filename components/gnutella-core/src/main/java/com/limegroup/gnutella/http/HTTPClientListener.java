package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.commons.httpclient.HttpMethod;

/**
 * Listener for events generated by the execution of an HTTP method
 * by HttpClient.
 */
public interface HTTPClientListener {
	public void requestComplete(HttpMethod method);
	public void requestFailed(HttpMethod method, IOException exc);
}
