package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.NBSocket;
import com.limegroup.gnutella.io.SocketFactory;

class LimitedSocketController extends SimpleSocketController {

    /**
     * The maximum number of concurrent connection attempts.
     */
    private final int MAX_CONNECTING_SOCKETS;
    
    /**
     * The current number of waiting socket attempts.
     */
    private int _socketsConnecting = 0;
    
    /**
     * Any non-blocking Requestors waiting on a pending socket.
     */
    private final List WAITING_REQUESTS = new LinkedList();
    
    /**
     * Constructs a new LimitedSocketController that only allows 'max'
     * number of connections concurrently.
     * @param max
     */
    LimitedSocketController(int max) {
        this.MAX_CONNECTING_SOCKETS = max;
    }
    

    /**
     * Connects to the given InetSocketAddress.
     * This will only connect if the number of connecting sockets has not
     * exceeded it's limit.  If we're above the limit already, then
     * the connection attempt will not take place until a prior attempt
     * completes (either by success or failure).
     * If observer is null, this will block until this connection attempt finishes.
     * Otherwise, observer will be notified of success or failure.
     */
    protected Socket connectPlain(InetSocketAddress addr, int timeout, ConnectObserver observer) throws IOException {
        NBSocket socket = SocketFactory.newSocket();
        
        if(observer == null) {
            // BLOCKING.
            waitForSocket();
            try {
                socket.connect(addr, timeout);
            } finally {
                releaseSocket();
            }
        } else {
            // NON BLOCKING
            if(addWaitingSocket(socket, addr, timeout, observer)) {
                socket.connect(addr, timeout, new DelegateConnector(observer));
            }
        }
        
        return socket;
    }

    /**
     * Removes the given observer from connecting.  If the attempt has already begun,
     * this will return false and the observer will eventually be notified.
     * Otherwise, this will return true and the observer will never be notified,
     * because the connection will never be attempted.
     */
    public synchronized boolean removeConnectObserver(ConnectObserver observer) {
        for(Iterator i = WAITING_REQUESTS.iterator(); i.hasNext(); ) {
            Requestor next = (Requestor)i.next();
            if(next.observer == observer) {
                i.remove();
                return true;
            // must handle proxy'd kinds also.
            } else if(next.observer instanceof ProxyUtils.ProxyConnector) {
                if(((ProxyUtils.ProxyConnector)next.observer).getDelegateObserver() == observer) {
                    i.remove();
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns the maximum number of concurrent attempts this will allow. */
    public int getNumAllowedSockets() {
        return MAX_CONNECTING_SOCKETS;
    }
    
    /**
     * Runs through any waiting Requestors and initiates a connection to them.
     */
    private void runWaitingRequests() {
        // We must connect outside of the lock, so as not to expose being locked to external
        // entities.
        List toBeProcessed = new ArrayList(Math.min(WAITING_REQUESTS.size(), Math.max(0, MAX_CONNECTING_SOCKETS - _socketsConnecting)));
        synchronized(this) {
            while(_socketsConnecting < MAX_CONNECTING_SOCKETS && !WAITING_REQUESTS.isEmpty()) {
                toBeProcessed.add(WAITING_REQUESTS.remove(0));
                _socketsConnecting++;
            }
        }
        
        for(int i = 0; i < toBeProcessed.size(); i++) {
            Requestor next = (Requestor)toBeProcessed.get(i);
            try {
                next.socket.connect(next.addr, next.timeout, new DelegateConnector(next.observer));
            } catch(IOException iox) {
                _socketsConnecting--;
                next.observer.shutdown();
            }
        }
    }
    
    /**
     * Determines if the given requestor can immediately connect.
     * If not, adds it to a pool of future connection-wanters.
     */
    private synchronized boolean addWaitingSocket(NBSocket socket, 
            InetSocketAddress addr, int timeout, ConnectObserver observer) {
        if (_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
            WAITING_REQUESTS.add(new Requestor(socket, addr, timeout, observer));
            return false;
        } else {
            _socketsConnecting++;
            return true;
        }
    }
    
    /**
     * Waits until we're allowed to do an active outgoing socket connection.
     */
    private synchronized void waitForSocket() throws IOException {
        while (_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
            try {
                wait();
            } catch (InterruptedException ix) {
                throw new IOException(ix.getMessage());
            }
        }
        _socketsConnecting++;
    }
    
    /**
     * Notification that a socket has been released.
     * 
     * If there are waiting non-blocking requests, spawns starts a new connection for them.
     */
    private void releaseSocket() {
        // Release this slot.
        synchronized(this) {
            _socketsConnecting--;
        }
        
        // See if any non-blocking requests are queued.
        runWaitingRequests();
        
        // If there's room, notify blocking requests.
        synchronized(this) {
            if(_socketsConnecting < MAX_CONNECTING_SOCKETS) {
                notifyAll();
            }
        }
    }
    
    /** A ConnectObserver to maintain the _socketsConnecting variable. */
    private class DelegateConnector implements ConnectObserver {
        private final ConnectObserver delegate;
        DelegateConnector(ConnectObserver observer) {
            delegate = observer;
        }
        
        public void handleConnect(Socket s) throws IOException {
            releaseSocket();
            delegate.handleConnect(s);
        }
        
        public void shutdown()  {
            releaseSocket();
            delegate.shutdown();
        }
        
        // unused.
        public void handleIOException(IOException x) {}
    }

    /** Simple struct to hold data for non-blocking waiting requests. */
    private static class Requestor {
        private final InetSocketAddress addr;
        private final int timeout;
        private final NBSocket socket;
        private final ConnectObserver observer;
        Requestor(NBSocket socket, InetSocketAddress addr, int timeout, ConnectObserver observer) {
            this.socket = socket;
            this.addr = addr;
            this.timeout = timeout;
            this.observer = observer;
        }
    }
    
}
