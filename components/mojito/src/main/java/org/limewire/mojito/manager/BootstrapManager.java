/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.manager;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.exceptions.CollisionException;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.exceptions.DHTTimeoutException;
import org.limewire.mojito.handler.response.FindNodeResponseHandler;
import org.limewire.mojito.handler.response.PingResponseHandler;
import org.limewire.mojito.handler.response.PingResponseHandler.PingIterator;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindNodeResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.BootstrapResult.ResultType;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.CollectionUtils;

/**
 * The BootstrapManager manages the entire bootstrap process.
 */
public class BootstrapManager extends AbstractManager<BootstrapResult> {
    
    private static final Log LOG = LogFactory.getLog(BootstrapManager.class);
    
    /**
     * The BootstrapFuture object that will contain the result
     * of our bootstrap.
     * LOCKING: this
     */
    private BootstrapFuture future;
    
    /** 
     * A flag for whether or not we're bootstrapped 
     */
    private boolean bootstrapped = false;
    
    public BootstrapManager(Context context) {
        super(context);
    }
    
    /**
     * Returns true if this Node is currently bootstrapping
     */
    public synchronized boolean isBootstrapping() {
        // A Future that was cancelled is also done
        if (future == null || future.isDone() /*|| future.isCancelled() */) {
            return false;
        }
        
        return true; 
    }
    
    /**
     * Returns true if this Node has bootstrapped successfully
     */
    public synchronized boolean isBootstrapped() {
        return bootstrapped;
    }
    
    /**
     * An internal method to set the bootsrapped flag.
     * Meant for internal use only.
     */
    public synchronized void setBootstrapped(boolean bootstrapped) {
        this.bootstrapped = bootstrapped;
    }
    
    /**
     * Stops bootstrapping if there are any bootstrapping processes active
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }
    
    /**
     * Deregisters (kills) the currently active bootstrap process
     */
    private void deregister() {
        stop();
    }
    
    /**
     * Bootstraps the local Node from the given Contact
     */
    public DHTFuture<BootstrapResult> bootstrap(Contact node) {
        if (node == null) {
            throw new NullPointerException("Contact is null");
        }

        if (node.equals(context.getLocalNode())) {
            throw new IllegalArgumentException("Cannot bootstrap from local Node");
        }

        // Make sure there is only one bootstrap process active!
        // Having parallel bootstrap proccesses is too expensive!
        deregister();
        
        // Bootstrap...
        BootstrapProcess process = new BootstrapProcess(node);
        BootstrapFuture future = new BootstrapFuture(process);
        synchronized (this) {
            this.future = future;
        }
        context.getDHTExecutorService().execute(future);

        return future;
    }
    
    /**
     * Bootstraps the local Node from the given Contact
     */
    public DHTFuture<BootstrapResult> bootstrap(Set<? extends SocketAddress> dst) {
        if (dst == null) {
            throw new NullPointerException("Set<SocketAddress> is null");
        }
        
        // Make sure there is only one bootstrap process active!
        // Having parallel bootstrap proccesses is too expensive!
        deregister();
        
        // Bootstrap...
        PingIterator pinger = new PingIteratorFactory.SocketAddressPinger(dst);
        FindContactProcess process = new FindContactProcess(pinger);
        BootstrapFuture future = new BootstrapFuture(process);
        synchronized (this) {
            this.future = future;
        }
        context.getDHTExecutorService().execute(future);

        return future;
    }
    
    private class FindContactProcess implements Callable<BootstrapResult> {
        
        private final PingIterator pinger;
        
        private FindContactProcess(PingIterator pinger) {
            this.pinger = pinger;
        }
        
        public BootstrapResult call() throws InterruptedException, DHTException {
            PingResponseHandler pingHandler = new PingResponseHandler(context, pinger);
            pingHandler.setMaxErrors(0); // Do not retry
            
            Contact node = pingHandler.call().getContact();
            BootstrapProcess bootstrapProcess = new BootstrapProcess(node);
            return bootstrapProcess.call();
        }
    }
    
    /**
     * The bootstrap process looks like:
     * 
     *     0) Find a Node that's connected to the DHT
     * +--->
     * |   1) Lookup own Node ID
     * |   2) Refresh all Buckets with prefixed random IDs
     * +---3) Prune RouteTable and restart if too many errors in #2
     *     4) Done
     */
    private class BootstrapProcess implements Callable<BootstrapResult> {
        
        private long phaseOne;
        private long phaseTwo;
        
        private boolean retriedToBootstrap = false;
        
        private final Contact node;
        
        public BootstrapProcess(Contact node) {
            this.node = node;
        }
        
        public BootstrapResult call() throws InterruptedException, DHTException {
            
            ResultType type = ResultType.BOOTSTRAP_FAILED;
            
            if (bootstrap()) {
                setBootstrapped(true);
                type = ResultType.BOOTSTRAP_SUCCEEDED;
            }
            
            return new BootstrapResult(node, phaseOne, phaseTwo, type);
        }
        
        /**
         * Tries to bootstrap the local Node and returns true on
         * success. If it fails due to a stale RouteTable it will
         * prune the RouteTable and tries it again.
         */
        private boolean bootstrap() throws InterruptedException, DHTException {
            try {
                if (bootstrap(node)) {
                    return true;
                }
            } catch (StaleRouteTableException e) {
                // The RouteTable is stale! Remove all non-alive Contacts,
                // rebuild the RouteTable and start over!
                context.getRouteTable().purge();
                return bootstrap(node);
            }
            
            return false;
        }
        
        /**
         * Bootstrap from the given Node
         */
        private boolean bootstrap(Contact node) throws InterruptedException, DHTException {
            // Begin with a lookup for the local Node ID
            long startPhaseOne = System.currentTimeMillis();
            FindNodeResult result = null;
            while(true) {
                try {
                    result = phaseOne(node);
                    break;
                } catch (CollisionException err) {
                    LOG.error("CollisionException", err);
                    context.changeNodeID();
                    // continue
                }
            }
            phaseOne += (System.currentTimeMillis() - startPhaseOne);
            
            // Phase one didn't work at all?
            if (result == null) {
                return false;
            }
            
            // Make sure we found some Nodes
            Collection<? extends Contact> path = result.getPath();
            if (path == null || path.isEmpty()) {
                return false;
            }
            
            // But other than our local Node
            if (path.size() == 1 && path.contains(context.getLocalNode())) {
                return false;
            }
            
            // We're essentially bootstrapped now but to optimize
            // our lookups we do a full bucket refresh if possible
            long startPhaseTwo = System.currentTimeMillis();
            boolean foundNewContacts = phaseTwo(node);
            phaseTwo += (System.currentTimeMillis() - startPhaseTwo);
            
            return foundNewContacts;
        }
        
        /**
         * Do a lookup for myself (Phase one)
         */
        private FindNodeResult phaseOne(Contact node) 
                throws InterruptedException, DHTException {
            
            FindNodeResponseHandler handler 
                = new FindNodeResponseHandler(context, node, context.getLocalNodeID());
            FindNodeResult result = handler.call();
            
            // Ping all Contacts that have our Node ID. If any
            // of them responds then change our Node ID and
            // try again!
            Collection<? extends Contact> collsions = result.getCollisions();
            if (!collsions.isEmpty()) {
                try {
                    PingResult pong = context.collisionPing(
                            CollectionUtils.toSet(collsions)).get();
                    Contact collidesWith = pong.getContact();
                    throw new CollisionException(collidesWith, 
                        context.getLocalNode() + " collides with " + collidesWith);
                } catch (ExecutionException err) {
                    Throwable cause = err.getCause();
                    if (cause instanceof DHTTimeoutException) {
                        // Ignore, everything is fine! Nobody did respond!
                        LOG.info("DHTTimeoutException", cause);
                    } else if (cause instanceof DHTException) {
                        throw (DHTException)cause;
                    } else {
                        throw new DHTException(cause);
                    }
                }
            }
            
            return result;
        }

        /**
         * Refresh all Buckets (Phase two)
         * 
         * When we detect that the routing table is stale, we purge it
         * and start the bootstrap all over again. 
         * A stale routing table can be detected by a high number of failures
         * during the lookup (alive hosts to expected result set size ratio).
         * Note: this only applies to routing tables with more than 1 buckets,
         * i.e. routing tables that have more than k nodes.
         */
        private boolean phaseTwo(Contact node) throws InterruptedException, DHTException {
            
            boolean foundNewContacts = false;
            int routeTableFailureCount = 0;
            
            List<KUID> randomId = context.getRouteTable().getRefreshIDs(true);
            if(randomId.isEmpty()) {
                return true;
            }
            
            int maxBootstrapFailures = KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue();
            for (KUID nodeId : randomId) {
                FindNodeResponseHandler handler 
                    = new FindNodeResponseHandler(context, nodeId);
                FindNodeResult result = handler.call();
                routeTableFailureCount += result.getRouteTableFailureCount();
                        
                if (routeTableFailureCount >= maxBootstrapFailures) {
                    
                    // Did it happen again? If so give up!
                    if (retriedToBootstrap) {
                        return false;
                    }
                    
                    // Fire a StaleRouteTableException otherwise...
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Too many failures: " + routeTableFailureCount 
                                + ". Retrying bootstrap from phase 2");
                    }
                    
                    retriedToBootstrap = true;
                    throw new StaleRouteTableException();
                    
                } else if (!foundNewContacts && !result.getPath().isEmpty()) {
                    foundNewContacts = true;
                }
            }
            return foundNewContacts;
        }
    }
    
    /*private boolean determinateIfBootstrapped() {
        if (bootstrapped) {
            return true;
        }
        
        RouteTable routeTable = context.getRouteTable();
        synchronized (routeTable) {
            routeTable.purge();
            List<Contact> active = routeTable.getActiveContacts();
            int alive = 0;
            for (Contact node : active) {
                if (node.isAlive()) {
                    alive++;
                }
            }
            
            float ratio = ((float)alive)/((float)active.size());
            System.out.println("ratio: " + ratio);
            return ratio >= 0.5f;
        }
    }/*
    
    /**
     * A bootstrap specific implementation of DHTFuture
     */
    private class BootstrapFuture extends DHTFutureTask<BootstrapResult> {
        
        private BootstrapFuture(Callable<BootstrapResult> task) {
            super(task);
        }
        
        @Override
        protected void deregister() {
            BootstrapManager.this.deregister();
        }
    }
    
    /**
     * Thrown to indicate our RouteTable is stale (a lot of dead contacts)
     */
    @SuppressWarnings("serial")
    private static class StaleRouteTableException extends DHTException {
        
    }
}
