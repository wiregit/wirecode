/**
 * 
 */
package org.limewire.nio.http;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;

class HttpSessionRequest implements SessionRequest {

    private SocketAddress remoteAddress;
    private SocketAddress localAddress;
    private Object attachment;
    private SessionRequestCallback callback;

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
        return 0;
    }

    public IOException getException() {
        // TODO Auto-generated method stub
        return null;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public IOSession getSession() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCompleted() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setCallback(SessionRequestCallback callback) {
        this.callback = callback;
        
    }

    public void setConnectTimeout(int timeout) {
        throw new UnsupportedOperationException(); 
    }

    public void waitFor() throws InterruptedException {
        // TODO Auto-generated method stub
        
    }
    
}
