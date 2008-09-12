package com.limegroup.gnutella.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpParams;
import org.limewire.collection.Cancellable;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.nio.observer.Shutdownable;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;


/**
 * Default implementation of <tt>HttpExecutor</tt>.
 */
@Singleton
public class DefaultHttpExecutor implements HttpExecutor {

	private static final ExecutorService POOL = 
        ExecutorsHelper.newThreadPool("HttpClient pool");
    private final Provider<LimeHttpClient> clientProvider;

    @Inject
    public DefaultHttpExecutor(Provider<LimeHttpClient> clientProvider) {
        this.clientProvider = clientProvider;
    }
	
    public Shutdownable execute(HttpUriRequest method, HttpParams params, HttpClientListener listener) {
		return execute(method, params, listener, POOL);
	}

	public Shutdownable execute(final HttpUriRequest method, final HttpParams params, final HttpClientListener listener,
			ExecutorService executor) {
		
		Runnable r = new Runnable() {
			public void run() {
				performRequest(method, params, listener);		
			}
		};
		executor.execute(r);
		return new Aborter(method);
	}
	
	private class Aborter implements Shutdownable {
		private final AbortableHttpRequest toAbort;
		Aborter(HttpUriRequest toAbort) {
            if(toAbort instanceof AbortableHttpRequest) {
                this.toAbort = (AbortableHttpRequest)toAbort;
            } else {
                this.toAbort = null;
            }
        }
		
        public void shutdown() {
            if(toAbort != null) {
                 toAbort.abort();
            }
        }
    }
	
	public void releaseResources(HttpResponse response) {
        HttpClientUtils.releaseConnection(response);
	}

	public Shutdownable executeAny(HttpClientListener listener, 
                        		   ExecutorService executor, 
                        		   Iterable<? extends HttpUriRequest> methods,
                                   HttpParams params,
                                   Cancellable canceller) {
		MultiRequestor r = new MultiRequestor(listener, methods, params, canceller);
		executor.execute(r);
		return r;
	}
	
	/**
     * Performs a single request.
     * Returns true if no more requests should be processed,
     * false if another request should be processed.
     */
	private boolean performRequest(HttpUriRequest method, HttpParams params, HttpClientListener listener) {
	    // If we aren't allowed to do this request, skip to the next.
	    if(!listener.allowRequest(method)) {
	        return false;
	    }
	    
		LimeHttpClient client = clientProvider.get();
        if(params != null) {
            client.setParams(params);
        }

        HttpResponse response;
        try {
			response = client.execute(method);
		} catch (IOException failed) {
			return !listener.requestFailed(method, null, failed);
		} catch (HttpException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            return !listener.requestFailed(method, null, ioe);
        }
		catch (IllegalStateException ise) {
		    // bug in httpclient lib, LWC-1637, do not forward port
		    IOException ioe = new IOException();
            ioe.initCause(ise);
            return !listener.requestFailed(method, null, ioe);
		}

        return !listener.requestComplete(method, response);
	}
	
    /** Runs all requests until the listener told it to not do anymore. */
	private class MultiRequestor implements Runnable, Shutdownable {
		private boolean shutdown;
		private HttpUriRequest currentMethod;
		private final Iterable<? extends HttpUriRequest> methods;
		private final HttpClientListener listener;
        private HttpParams params;
        private final Cancellable canceller;
		
		MultiRequestor(HttpClientListener listener, 
				Iterable<? extends HttpUriRequest> methods, HttpParams params, Cancellable canceller) {
			this.methods = methods;
			this.listener = listener;
            this.params = params;
            this.canceller = canceller;
		}
		
		public void run() {
			for (HttpUriRequest m : methods) {
				synchronized(this) {
					if (shutdown)
						return;
					currentMethod = m;
				}
				if (canceller.isCancelled())
					return;
				if (performRequest(m, params, listener))
					return;
			}
		}
		
		public void shutdown() {
			HttpUriRequest m;
			synchronized (this) {
				shutdown = true;
				m = currentMethod;
			}
            if(m instanceof AbortableHttpRequest) {
                ((AbortableHttpRequest)m).abort();
            }
        }
	}

}
