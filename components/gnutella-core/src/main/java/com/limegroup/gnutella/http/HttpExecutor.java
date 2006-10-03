package com.limegroup.gnutella.http;

import org.apache.commons.httpclient.HttpMethod;

import com.limegroup.gnutella.util.ThreadPool;

/**
 * Something that executes HTTP requests using the http client library.
 */
public interface HttpExecutor {
	/**
	 * Execute the provided <tt>HttpMethod</tt> and notifies the
	 * provided <tt>HTTPClientListener</tt> using a default
	 * <tt>ThreadPool</tt>
	 */
	public void execute(HttpMethod method, HTTPClientListener listener, int timeout);
	
	/**
	 * Execute the provided <tt>HttpMethod</tt> using the provided <tt>ThreadPool</tt>
	 * and notifies the provided <tt>HTTPClientListener</tt> on the same thread.
	 */
	public void execute(HttpMethod method, 
			HTTPClientListener listener,
			int timeout,
			ThreadPool executor);
}
