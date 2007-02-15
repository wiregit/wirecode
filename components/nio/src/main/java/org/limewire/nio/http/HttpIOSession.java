/**
 * 
 */
package org.limewire.nio.http;

import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionBufferStatus;
import org.limewire.nio.NIOSocket;

public class HttpIOSession implements IOSession {

    private final Map<String, Object> attributes;
    
    private SessionBufferStatus bufferStatus;
    private int socketTimeout;

    private NIOSocket socket;

    private HttpChannel channel;

    private int eventMask;
    
    public HttpIOSession(NIOSocket socket) {
        this.attributes = Collections.synchronizedMap(new HashMap<String, Object>());
        this.socketTimeout = 0;
        this.socket = socket;
    }

    public void setHttpChannel(HttpChannel channel) {
        this.channel = channel;
    }

    public ByteChannel channel() {
        return channel;
    }

    public void close() {
        socket.close();
        channel.shutdown();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public SessionBufferStatus getBufferStatus() {
        return bufferStatus;
    }

    public SocketAddress getLocalAddress() {
        return socket.getLocalSocketAddress();
    }

    public SocketAddress getRemoteAddress() {
        return socket.getRemoteSocketAddress();
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }

    public void setAttribute(String name, Object obj) {
        attributes.put(name, obj);
    }

    public void setBufferStatus(SessionBufferStatus status) {
        this.bufferStatus = status;
    }

    public int getEventMask() {
        return eventMask;
    }
    
    public void setEventMask(int ops) {
        updateEventMask(ops);
    }
    
    private void updateEventMask(int ops) {
        if (isClosed()) {
            return;
        }
        this.eventMask = ops;
        channel.requestRead((ops & EventMask.READ) != 0);
        channel.requestWrite((ops & EventMask.WRITE) != 0);
    }

    public void setEvent(int op) {
        updateEventMask(eventMask | op);
    }
    
    public void clearEvent(int op) {
        updateEventMask(eventMask & ~op);
    }

    public void setSocketTimeout(int timeout) {
        this.socketTimeout = timeout;
    }

    public boolean hasBufferedInput() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasBufferedOutput() {
        // TODO Auto-generated method stub
        return false;
    }
    
}