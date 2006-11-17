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
	
	public Shutdownable execute(HttpMethod method, HttpClientListener listener, int timeout) {
		return execute(method, listener, timeout, POOL);
	}

	public Shutdownable execute(final HttpMethod method, final HttpClientListener listener,
			final int timeout,
			ThreadPool executor) {
		
		Runnable r = new Runnable() {
			public void run() {
				performRequest(method, listener, timeout);		
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
	
	public void releaseResources(HttpMethod method) {
		method.releaseConnection();
	}

	public Shutdownable executeAny(final HttpClientListener listener, 
			final int timeout, ThreadPool executor, 
			final Iterable<? extends HttpMethod> methods) {
		MultiRequestor r = new MultiRequestor(listener, timeout, methods);
		executor.invokeLater(r);
		return r;
	}
	
	
	private boolean performRequest(HttpMethod method, HttpClientListener listener, int timeout) {
		HttpClient client = HttpClientManager.getNewClient(timeout, timeout);
		try {
			HttpClientManager.executeMethodRedirecting(client, method);
		} catch (IOException failed) {
			listener.requestFailed(method, failed);
			return false;
		} 
		
		listener.requestComplete(method);
		return true;
	}
	
	private class MultiRequestor implements Runnable, Shutdownable {
		private volatile boolean shutdown;
		private volatile HttpMethod currentMethod;
		private final Iterable<? extends HttpMethod> methods;
		private final HttpClientListener listener;
		private final int timeout;
		
		MultiRequestor(HttpClientListener listener, int timeout, 
				Iterable<? extends HttpMethod> methods) {
			this.methods = methods;
			this.timeout = timeout;
			this.listener = listener;
		}
		
		public void run() {
			for (HttpMethod m : methods) {
				if (shutdown)
					return;
				currentMethod = m;
				if (performRequest(m, listener, timeout))
					return;
			}
		}
		
		public void shutdown() {
			shutdown = true;
			HttpMethod m = currentMethod;
			if (m != null)
				m.abort();
		}
	}

}
