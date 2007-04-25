/**
 * 
 */
package org.limewire.http;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionBufferStatus;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.Throttle;
import org.limewire.nio.channel.ThrottleWriter;

public class HttpIOSession implements IOSession {

    private static final Log LOG = LogFactory.getLog(HttpIOSession.class);
    
    private final Map<String, Object> attributes;

    private SessionBufferStatus bufferStatus;

    private int socketTimeout;

    private AbstractNBSocket socket;

    private HttpChannel channel;

    private int eventMask;

    private ThrottleWriter throttleWriter;

    public HttpIOSession(AbstractNBSocket socket) {
        if (socket == null) {
            throw new IllegalArgumentException();
        }

        this.attributes = Collections
                .synchronizedMap(new HashMap<String, Object>());
        this.socketTimeout = 0;
        this.socket = socket;
    }

    public void setHttpChannel(HttpChannel channel) {
        this.channel = channel;
    }

    public ByteChannel channel() {
        return (channel != null) ? channel : channel;
    }

    public void close() {
        socket.close();
        // XXX is this call required or did socket.close() already shutdown the
        // channel?
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
//        if ((op & EventMask.READ) != 0) {
//            System.out.println("read on");
//        }
//        if ((op & EventMask.WRITE) != 0) {
//            System.out.println("write on");
//        }
        updateEventMask(eventMask | op);
    }

    public void clearEvent(int op) {
//        if ((op & EventMask.READ) != 0) {
//            System.out.println("read off");
//        }
//        if ((op & EventMask.WRITE) != 0) {
//            System.out.println("write off");
//        }
        updateEventMask(eventMask & ~op);
    }

    public void setSocketTimeout(int timeout) {
        this.socketTimeout = timeout;
        if (socket != null) {
            try {
                socket.setSoTimeout(timeout);
            } catch (SocketException e) {
                LOG.warn("Could not set socket timeout", e);
            }
        }
    }

    public boolean hasBufferedInput() {
        return this.bufferStatus != null
                && this.bufferStatus.hasBufferedInput();
    }

    public boolean hasBufferedOutput() {
        return this.bufferStatus != null
                && this.bufferStatus.hasBufferedOutput();
    }

    public synchronized void setThrottle(Throttle throttle) {
        boolean enable = (throttle != null);
        if (enable == (this.throttleWriter != null)) {
            return;
        }

        if (enable) {
            this.throttleWriter = new ThrottleWriter(throttle);
            boolean interest = channel.isWriteInterest();
            channel.requestWrite(false);
            channel.setWriteChannel(this.throttleWriter);
            socket.setWriteObserver(channel);
            channel.requestWrite(interest);
        } else {
            socket.setWriteObserver(channel);
        }
    }

    public Socket getSocket() {
        return socket;
    }

}