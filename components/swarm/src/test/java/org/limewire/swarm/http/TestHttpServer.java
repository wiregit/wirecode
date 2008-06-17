
package org.limewire.swarm.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.mockup.RequestCount;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.params.HttpParams;

/**
 * Trivial test server based on HttpCore NIO
 * 
 * @author Oleg Kalnichevski
 */
public class TestHttpServer {

    private final DefaultListeningIOReactor ioReactor;
    private final HttpParams params;

    private volatile IOReactorThread thread;
    private ListenerEndpoint endpoint;
    
    private volatile CountDownLatch requestCount;
    
    public TestHttpServer(final HttpParams params) throws IOException {
        super();
        this.ioReactor = new DefaultListeningIOReactor(2, params);
        this.params = params;
    }

    public HttpParams getParams() {
        return this.params;
    }
    
    public void setRequestCount(final CountDownLatch requestCount) {
        this.requestCount = requestCount;
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.ioReactor.setExceptionHandler(exceptionHandler);
    }

    private void execute(final NHttpServiceHandler serviceHandler) throws IOException {
        IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(
                serviceHandler, 
                this.params);
        
        this.ioReactor.execute(ioEventDispatch);
    }
    
    public ListenerEndpoint getListenerEndpoint() {
        return this.endpoint;
    }

    public void setEndpoint(ListenerEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void start(final NHttpServiceHandler serviceHandler) {
        this.endpoint = this.ioReactor.listen(new InetSocketAddress(0));
        this.thread = new IOReactorThread(serviceHandler);
        this.thread.start();
    }
    
    public IOReactorStatus getStatus() {
        return this.ioReactor.getStatus();
    }
    
    public void join(long timeout) throws InterruptedException {
        if (this.thread != null) {
            this.thread.join(timeout);
        }
    }
    
    public Exception getException() {
        if (this.thread != null) {
            return this.thread.getException();
        } else {
            return null;
        }
    }
    
    public void shutdown() throws IOException {
        this.ioReactor.shutdown();
        try {
            join(500);
        } catch (InterruptedException ignore) {
        }
    }
    
    private class IOReactorThread extends Thread {

        private final NHttpServiceHandler serviceHandler;
        
        private volatile Exception ex;
        
        public IOReactorThread(final NHttpServiceHandler serviceHandler) {
            super();
            this.serviceHandler = serviceHandler;
        }
        
        @Override
        public void run() {
            try {
                execute(this.serviceHandler);
            } catch (Exception ex) {
                this.ex = ex;
            }
        }
        
        public Exception getException() {
            return this.ex;
        }

    }    
    
}
