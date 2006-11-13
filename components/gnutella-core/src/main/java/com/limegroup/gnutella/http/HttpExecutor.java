package com.limegroup.gnutella.http;

import org.apache.commons.httpclient.HttpMethod;

import com.limegroup.gnutella.io.Shutdownable;
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
	public Shutdownable execute(HttpMethod method, HTTPClientListener listener, int timeout);
	
	/**
	 * Execute the provided <tt>HttpMethod</tt> using the provided <tt>ThreadPool</tt>
	 * and notifies the provided <tt>HTTPClientListener</tt> on the same thread.
	 */
	public Shutdownable execute(HttpMethod method, 
			HTTPClientListener listener,
			int timeout,
			ThreadPool executor);
	
	/**
	 * Tries to execute the provided HTTPMethods until one succeeds.
	 */
	public Shutdownable executeAny(HTTPClientListener listener,
			int timeout,
			ThreadPool executor,
			Iterable<? extends HttpMethod> methods);
	
	/**
	 * Release any resources held by the provided method.
	 * The users of this class must call this method once they're done
	 * processing their HttpMethod object.
	 */
	public void releaseResources(HttpMethod method);
}
