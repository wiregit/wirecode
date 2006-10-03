package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.util.DefaultThreadPool;
import com.limegroup.gnutella.util.ThreadPool;

/**
 * Default implementation of <tt>HttpExecutor</tt>, uses internal
 * <tt>DefaultThreadPool</tt>
 */
public class DefaultHttpExecutor implements HttpExecutor {

	private static final DefaultThreadPool POOL = 
		new DefaultThreadPool("HttpClient pool", true);
	
	public Shutdownable execute(HttpMethod method, HTTPClientListener listener, int timeout) {
		return execute(method, listener, timeout, POOL);
	}

	public Shutdownable execute(final HttpMethod method, final HTTPClientListener listener,
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
		return new Aborter(method);
	}
	
	private class Aborter implements Shutdownable {
		private final HttpMethod toAbort;
		Aborter(HttpMethod toAbort) {
			this.toAbort = toAbort;
		}
		
		public void shutdown() {
			toAbort.abort();
		}
	}

}
