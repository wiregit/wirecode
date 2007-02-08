package org.limewire.nio.http;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;

/** 
 * Based on {@link org.apache.http.impl.nio.reactor.SessionRequestImpl}.
 */
public class HttpSessionRequest implements SessionRequest {

    private SocketAddress remoteAddress;
    private SocketAddress localAddress;
    private Object attachment;
    private SessionRequestCallback callback;
    private Object finishLock = new Object();
    private int connectTimeout;
    private IOException exception;
    private IOSession session;
    private volatile boolean completed;

    public HttpSessionRequest(SocketAddress remoteAddress,
            SocketAddress localAddress, Object attachment) {
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
        this.attachment = attachment;
    }

    public void cancel() {
        throw new UnsupportedOperationException(); 
    }

    public Object getAttachment() {
        return attachment;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public synchronized IOException getException() {
        return exception;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public synchronized IOSession getSession() {
        return session;
    }

    public boolean isCompleted() {
        return completed;
    }

    public synchronized void setCallback(final SessionRequestCallback callback) {
        this.callback = callback;
        if (this.completed) {
            if (this.exception != null) {
                callback.failed(this);
            } else if (this.session != null) {
                callback.completed(this);
            }
        }
    }

    public void setConnectTimeout(int timeout) {
        this.connectTimeout = timeout; 
    }

    public void waitFor() throws InterruptedException {
        if (this.completed) {
            return;
        }
        synchronized (this) {
            while (!this.completed) {
                wait();
            }
        }
    }

    public void connected(final IOSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session may not be null");
        }
        if (this.completed) {
            throw new IllegalStateException("Session request already completed");
        }
        this.completed = true;
        synchronized (this) {
            this.session = session;
            if (this.callback != null) {
                this.callback.completed(this);
            }
            notifyAll();
        }
    }
    
    public void failed(IOException exception) {
        if (exception == null) {
            throw new IllegalArgumentException();
        }
        if (this.completed) {
            throw new IllegalStateException("Session request already completed");
        }
        this.completed = true;
        synchronized (this) {
            this.exception = exception;
            if (this.callback != null) {
                this.callback.failed(this);
            }
            notifyAll();
        }
    }

    public void shutdown() {
        this.completed = true;
        
        if (callback != null) {
            callback.failed(this);
        }
        
        synchronized (finishLock) {
            finishLock.notifyAll();
        }
    }
    
}
