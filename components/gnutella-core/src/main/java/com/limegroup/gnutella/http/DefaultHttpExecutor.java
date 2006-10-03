package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

import com.limegroup.gnutella.util.DefaultThreadPool;
import com.limegroup.gnutella.util.ThreadPool;

/**
 * Default implementation of <tt>HttpExecutor</tt>, uses internal
 * <tt>DefaultThreadPool</tt>
 */
public class DefaultHttpExecutor implements HttpExecutor {

	private static final DefaultThreadPool POOL = 
		new DefaultThreadPool("HttpClient pool", true);
	
	public void execute(HttpMethod method, HTTPClientListener listener, int timeout) {
		execute(method, listener, timeout, POOL);
	}

	public void execute(final HttpMethod method, final HTTPClientListener listener,
			final int timeout,
			ThreadPool executor) {
		
		Runnable r = new Runnable() {
			public void run() {
				HttpClient client = HttpClientManager.getNewClient(
						timeout, timeout);
				try {
					client.executeMethod(method);
				} catch (IOException failed) {
					listener.requestFailed(method, failed);
					return;
				} finally {
					method.releaseConnection();
				}
				
				listener.requestComplete(method);
			}
		};
		executor.invokeLater(r);
	}

}
