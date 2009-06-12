package org.limewire.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ProxyManager.ProxyConnector;
import org.limewire.nio.NBSocket;
import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionRequirement;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A SocketController that does everything SimpleSocketController does,
 * except limits the number of outgoing attempts at any given moment.
 * 
 * Connection attempts beyond the limit will be queued until space
 * is available for connecting.
 */
@Singleton
class LimitedSocketController extends AbstractSocketController {
    
    private final static Log LOG = LogFactory.getLog(LimitedSocketController.class);
    
    private static final int DEFAULT_MAX_CONNECTING_SOCKETS = 4;

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
    private final List<Requestor> WAITING_REQUESTS = new LinkedList<Requestor>();

    /**
     * Inspections related to the queue of connection attempt requestors:
     *
     *  1. Maximum number of requests in the queue
     *  2. Num of requests in queue cancelled before conn attempted
     *  3. Total requests that have been gone thru the queue
     *  4. Average time spent waiting in the queue
     *  5. Maximum time 1 request has spent in the queue
     *
     */
    @InspectionPoint(value = "limited-socket-stats", requires = InspectionRequirement.OS_WINDOWS)
    private final LimitedSocketInspectable inspectable = new LimitedSocketInspectable();

    /**
     * A histogram representing the number of requests in the waiting queue upon connection attempt
     * (inspection gathered at beginning of call to {@link #connectPlain})
     */
    @InspectionPoint(value = "limited-socket-req", requires = InspectionRequirement.OS_WINDOWS)
    private final InspectionHistogram<Integer> requestsInQueue = new InspectionHistogram<Integer>();

    /**
     * Constructs a new LimitedSocketController that only allows 'max'
     * number of connections concurrently.
     */
    @Inject
    LimitedSocketController(ProxyManager proxyManager, SocketBindingSettings socketBindingSettings) {
        this(proxyManager, socketBindingSettings, DEFAULT_MAX_CONNECTING_SOCKETS);
    }
    
    LimitedSocketController(ProxyManager proxyManager, SocketBindingSettings socketBindingSettings, int maxConnectingSockets) {
        super(proxyManager, socketBindingSettings);
        this.MAX_CONNECTING_SOCKETS = maxConnectingSockets;
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
    @Override
    protected Socket connectPlain(InetSocketAddress localAddr,
            NBSocketFactory factory, InetSocketAddress addr, int timeout,
            ConnectObserver observer) throws IOException {
        NBSocket socket = factory.createSocket();
        bindSocket(socket, localAddr);

        requestsInQueue.count(getNumWaitingSockets());
        
        if(observer == null) {
            if(LOG.isDebugEnabled()) {
                int waiting = getNumWaitingSockets();
                LOG.debug(waiting + " waiting for sockets (blocking)");
            }
            // BLOCKING.
            waitForSocket();
            if(LOG.isDebugEnabled()) {
                String ipp = addr.getAddress().getHostAddress() +
                    ":" + addr.getPort();
                LOG.debug("Connecting to " + ipp + " (blocking)");
            }
            try {
                socket.connect(addr, timeout);
            } finally {
                releaseSocket();
            }
        } else {
            // NON BLOCKING
            if(addWaitingSocket(socket, addr, timeout, observer)) {
                if(LOG.isDebugEnabled()) {
                    String ipp = addr.getAddress().getHostAddress() +
                        ":" + addr.getPort();
                    LOG.debug("Connecting to " + ipp + " (non-blocking)");
                }
                socket.connect(addr, timeout, new DelegateConnector(observer));
            } else {
                if(LOG.isDebugEnabled()) {
                    int waiting = getNumWaitingSockets();
                    LOG.debug(waiting + " waiting for sockets (non-blocking)");
                }
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
    @Override
    public synchronized boolean removeConnectObserver(ConnectObserver observer) {
        for(Iterator<Requestor> i = WAITING_REQUESTS.iterator(); i.hasNext(); ) {
            Requestor next = i.next();
            if(next.observer == observer) {
                i.remove();
                return true;
            // must handle proxy'd kinds also.
            } else if(next.observer instanceof ProxyConnector) {
                if(((ProxyConnector)next.observer).getDelegateObserver() == observer) {
                    i.remove();
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns the maximum number of concurrent attempts this will allow. */
    @Override
    public int getNumAllowedSockets() {
        return MAX_CONNECTING_SOCKETS;
    }
    
    /** Returns the number of sockets waiting. */
    @Override
    public synchronized int getNumWaitingSockets() {
        return WAITING_REQUESTS.size();
    }
    
    /**
     * Runs through any waiting Requestors and initiates a connection to them.
     */
    private void runWaitingRequests() {
        // We must connect outside of the lock, so as not to expose being locked to external
        // entities.
        List<Requestor> toBeProcessed = new ArrayList<Requestor>(Math.min(WAITING_REQUESTS.size(),
                                           Math.max(0, MAX_CONNECTING_SOCKETS - _socketsConnecting)));
        synchronized(this) {
            while(_socketsConnecting < MAX_CONNECTING_SOCKETS && !WAITING_REQUESTS.isEmpty()) {
                Requestor next = WAITING_REQUESTS.remove(0);
                if(!next.socket.isClosed()) {
                    toBeProcessed.add(next);
                    _socketsConnecting++;
                } else {
                    inspectable.incrementCancelledRequestCount();
                }
            }
        }
        
        for(int i = 0; i < toBeProcessed.size(); i++) {
            Requestor next = toBeProcessed.get(i);
            inspectable.addReadyRequest(next);
            if(LOG.isDebugEnabled()) {
                String ipp = next.addr.getAddress().getHostAddress() +
                    ":" + next.addr.getPort();
                LOG.debug("Connecting to " + ipp + " (waiting)");
            }
            next.socket.setShutdownObserver(null);
            next.socket.connect(next.addr, next.timeout, new DelegateConnector(next.observer));
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
            inspectable.setMaxConnReqInQueueIfNecessary(getNumWaitingSockets());
            socket.setShutdownObserver(new RemovalObserver(observer));
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
            if(LOG.isDebugEnabled()) {
                LOG.debug("Releasing socket, " +
                        _socketsConnecting + " connecting, " +
                        getNumWaitingSockets() + " waiting");
            }
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
    
    /**
     * An observer that is notified if the socket is shutdown while it is
     * in the requesting list.
     */
    private class RemovalObserver implements Shutdownable {
        private final ConnectObserver delegate;
        RemovalObserver(ConnectObserver observer) {
            this.delegate = observer;
        }
        
        public void shutdown() {
            if(removeConnectObserver(delegate)) {
                inspectable.incrementCancelledRequestCount();
                delegate.shutdown();
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
        private final long creationTime;

        Requestor(NBSocket socket, InetSocketAddress addr, int timeout, ConnectObserver observer) {
            this.socket = socket;
            this.addr = addr;
            this.timeout = timeout;
            this.observer = observer;
            this.creationTime = System.currentTimeMillis();
        }
    }

    /** Inspections related to the queue of connection attempt requestors */
    private class LimitedSocketInspectable implements Inspectable {

        private int maxConnectRequestsInQueue = 0;    /** Maximum number of requests in the queue */
        private int numberOfCancelledRequests = 0;    /** Num of requests in queue cancelled before conn attempted */
        private int totalQueueRequestsProcessed = 0;  /** Total requests that have been gone thru the queue */
        private long maxTimeSpentInQueue = 0L;        /** Maximum time 1 request has spent in the queue */
        private long totalWaitTimeInQueue = 0L;       /** Total time all requests have spent in the queue */

        /** Maximum time 1 request has spent in the queue */

        public synchronized Object inspect() {
            Map<String,Object> ret = new HashMap<String,Object>();
            ret.put("req_processed", totalQueueRequestsProcessed);
            ret.put("max_requests_in_queue", maxConnectRequestsInQueue);
            ret.put("max_time_in_queue", maxTimeSpentInQueue);
            ret.put("req_cancelled", numberOfCancelledRequests);
            ret.put("total_time_in_queue", totalWaitTimeInQueue);
            return ret;
        }

        synchronized void setMaxConnReqInQueueIfNecessary(int newMax) {
            if (newMax > maxConnectRequestsInQueue) {
                maxConnectRequestsInQueue = newMax;
            }
        }

        synchronized void incrementCancelledRequestCount() {
            numberOfCancelledRequests++;
        }

        synchronized void addReadyRequest(Requestor request) {
            totalQueueRequestsProcessed++;

            long timeSpent = System.currentTimeMillis() - request.creationTime;
            totalWaitTimeInQueue += timeSpent;

            if (timeSpent > this.maxTimeSpentInQueue) {
                maxTimeSpentInQueue = timeSpent;
            }
        }
    }
    
}
