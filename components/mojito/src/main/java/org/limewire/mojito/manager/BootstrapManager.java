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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.OnewayExchanger;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.exceptions.DHTTimeoutException;
import org.limewire.mojito.handler.response.FindNodeResponseHandler;
import org.limewire.mojito.handler.response.PingResponseHandler;
import org.limewire.mojito.handler.response.PingResponseHandler.PingIterator;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindNodeResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.BootstrapResult.ResultType;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.settings.BootstrapSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.RouteTableUtils;

/**
 * The BootstrapManager manages the bootstrap process and determinates
 * whether or not the local Node is bootstrapped.
 */
public class BootstrapManager extends AbstractManager<BootstrapResult> {
    
    private static final Log LOG = LogFactory.getLog(BootstrapManager.class);
    
    /**
     * The BootstrapFuture object that will contain the result
     * of our bootstrap.
     * LOCKING: this
     */
    private BootstrapFuture future = null;
    
    /** 
     * A flag for whether or not we're bootstrapped 
     */
    private boolean bootstrapped = false;
    
    public BootstrapManager(Context context) {
        super(context);
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
     * Returns true if this Node is currently bootstrapping
     */
    public synchronized boolean isBootstrapping() {
        return future != null;
    }
    
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }
    
    /**
     * Tries to bootstrap the local Node from the given Contact
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
        stop();
        
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
     * Tries to bootstrap the local Node from any of the given SocketAddresses
     */
    public DHTFuture<BootstrapResult> bootstrap(Set<? extends SocketAddress> dst) {
        if (dst == null) {
            throw new NullPointerException("Set<SocketAddress> is null");
        }
        
        // Make sure there is only one bootstrap process active!
        // Having parallel bootstrap proccesses is too expensive!
        stop();
        
        // Bootstrap...
        BootstrapProcess process = new BootstrapProcess(dst);
        BootstrapFuture future = new BootstrapFuture(process);
        synchronized (this) {
            this.future = future;
        }
        context.getDHTExecutorService().execute(future);

        return future;
    }
    
    private class BootstrapFuture extends DHTFutureTask<BootstrapResult> {

        public BootstrapFuture(DHTTask<BootstrapResult> task) {
            super(context, task);
        }

        @Override
        protected void done() {
            stop();
        }
    }
    
    /**
     * The bootstrap process looks like:
     * 
     *     0) Find a Node that's connected to the DHT
     * +--->
     * |   1) Lookup own Node ID
     * |---2) If there are any Node ID collisions then check 'em,
     * |      change or Node ID is necessary and start over
     * |   3) Refresh all Buckets with prefixed random IDs
     * +---4) Prune RouteTable and restart if too many errors in #3
     *     5) Done
     */
    private class BootstrapProcess implements DHTTask<BootstrapResult> {

        private OnewayExchanger<BootstrapResult, ExecutionException> exchanger;
        
        private final List<DHTTask<?>> tasks = new ArrayList<DHTTask<?>>();
        
        private boolean retriedToBootstrap = false;
        
        private boolean foundNewContacts = false;
        
        private int routeTableFailureCount = 0;
        
        private boolean cancelled = false;
        
        private Iterator<KUID> randomIds;
        
        private Contact node;
        
        private Set<? extends SocketAddress> dst;
        
        private long startPhaseOne;
        
        private long startPhaseTwo;
        
        public BootstrapProcess(Contact node) {
            this.node = node;
        }
        
        public BootstrapProcess(Set<? extends SocketAddress> dst) {
            this.dst = dst;
        }
        
        public long getLockTimeout() {
            return NetworkSettings.BOOTSTRAP_TIMEOUT.getValue();
        }

        public void start(OnewayExchanger<BootstrapResult, 
                ExecutionException> exchanger) {
            
            if (exchanger == null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Starting ResponseHandler without an OnewayExchanger");
                }
                exchanger = new OnewayExchanger<BootstrapResult, ExecutionException>(true);
            }

            this.exchanger = exchanger;

            if (node == null) {
                findInitialContact();
            } else {
                findNearestNodes();
            }
        }
        
        private void findInitialContact() {
            OnewayExchanger<PingResult, ExecutionException> c 
                    = new OnewayExchanger<PingResult, ExecutionException>(true) {
                @Override
                public synchronized void setValue(PingResult value) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Found initial bootstrap Node: " + value);
                    }
                    
                    super.setValue(value);
                    handlePong(value);
                }

                @Override
                public synchronized void setException(ExecutionException exception) {
                    LOG.info("ExecutionException", exception);
                    super.setException(exception);
                    exchanger.setException(exception);
                }
            };
            
            PingResponseHandler handler = new PingResponseHandler(context, 
                    new PingIteratorFactory.SocketAddressPinger(dst));
            handler.setMaxErrors(0);
            start(handler, c);
        }
        
        private void handlePong(PingResult result) {
            this.node = result.getContact();
            findNearestNodes();
        }
        
        private void findNearestNodes() {
            OnewayExchanger<FindNodeResult, ExecutionException> c 
                    = new OnewayExchanger<FindNodeResult, ExecutionException>(true) {
                @Override
                public synchronized void setValue(FindNodeResult value) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Found nearest Nodes: " + value);
                    }
                    
                    super.setValue(value);
                    handleNearestNodes(value);
                }

                @Override
                public synchronized void setException(ExecutionException exception) {
                    LOG.info("ExecutionException", exception);
                    super.setException(exception);
                    exchanger.setException(exception);
                }
            };
    
            startPhaseOne = System.currentTimeMillis();
            FindNodeResponseHandler handler = new FindNodeResponseHandler(
                    context, node, context.getLocalNodeID());
            start(handler, c);
        }
        
        private void handleNearestNodes(FindNodeResult result) {
            Collection<? extends Contact> collisions = result.getCollisions();
            if (!collisions.isEmpty()) {
                checkCollisions(collisions);
            } else {
                Collection<? extends Contact> path = result.getPath();
                
                // Make sure we found some Nodes
                if (path == null || path.isEmpty()) {
                    bootstrapped(false);
                    
                // But other than our local Node
                } else if (path.size() == 1 
                        && path.contains(context.getLocalNode())) {
                    bootstrapped(false);
                
                // Great! Everything is fine and continue with
                // refreshing/filling up the RouteTable by doing
                // lookups for random IDs
                } else {
                    refreshAllBuckets();
                }
            }
        }
        
        private void checkCollisions(Collection<? extends Contact> collisions) {
            OnewayExchanger<PingResult, ExecutionException> c 
                    = new OnewayExchanger<PingResult, ExecutionException>(true) {
                @Override
                public synchronized void setValue(PingResult value) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error(context.getLocalNode() + " collides with " + value.getContact());
                    }
                    
                    super.setValue(value);
                    handleCollision(value);
                }

                @Override
                public synchronized void setException(ExecutionException exception) {
                    LOG.info("ExecutionException", exception);
                    super.setException(exception);
                    
                    Throwable cause = exception.getCause();
                    if (cause instanceof DHTTimeoutException) {
                        // Ignore, everything is fine! Nobody did respond
                        // and we can keep our Node ID which is good!
                        // Continue with finding random Node IDs
                        refreshAllBuckets();

                    } else {
                        exchanger.setException(exception);
                    }
                }
            };
    
            Contact sender = ContactUtils.createCollisionPingSender(context.getLocalNode());
            PingIterator pinger = new PingIteratorFactory.CollisionPinger(
                    context, sender, CollectionUtils.toSet(collisions));
            
            PingResponseHandler handler 
                = new PingResponseHandler(context, sender, pinger);
            start(handler, c);
        }
        
        private void handleCollision(PingResult result) {
            // Change our Node ID
            context.changeNodeID();
            
            // Start over!
            findNearestNodes();
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
        private void refreshAllBuckets() {
            startPhaseTwo = System.currentTimeMillis();
            routeTableFailureCount = 0;
            foundNewContacts = false;
            
            Collection<KUID> ids = getBucketsToRefresh();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Buckets to refresh: " + CollectionUtils.toString(ids));
            }
            
            randomIds = ids.iterator();
            if (randomIds.hasNext()) {
                refreshBucket(randomIds.next());
            } else {
                bootstrapped(true);
            }
        }
        
        private Collection<KUID> getBucketsToRefresh() {
            int max = BootstrapSettings.MAX_BUCKETS_TO_REFRESH.getValue();
            if (max <= 0) {
                return Collections.emptySet();
            }
            
            List<KUID> bucketIds = CollectionUtils.toList(
                    context.getRouteTable().getRefreshIDs(true));
            
            // If there are less IDs than max then just return them
            if (bucketIds.size() < max) {
                return bucketIds;
            }
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Too many Buckets to refresh, reducing count from " 
                        + bucketIds.size() + " to " + max);
            }
            
            // Go through the list and select every nth element
            // We're prefer IDs that are further away because
            // 'findNearestNodes()' we've already good knowledge
            // of our nearby area...
            int offset = bucketIds.size() / max;
            List<KUID> refresh = new ArrayList<KUID>(max);
            for (int i = bucketIds.size()-1; i >= 0; i -= offset) {
                refresh.add(bucketIds.get(i));
                
                if (refresh.size() >= max) {
                    break;
                }
            }
            
            return refresh;
        }
        
        private void refreshBucket(KUID randomId) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Begin Bucket refresh for: " + randomId);
            }
            
            OnewayExchanger<FindNodeResult, ExecutionException> c 
                    = new OnewayExchanger<FindNodeResult, ExecutionException>(true) {
                @Override
                public synchronized void setValue(FindNodeResult value) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Finished Bucket refresh: " + value);
                    }
                    
                    super.setValue(value);
                    handleBucketRefresh(value);
                }

                @Override
                public synchronized void setException(ExecutionException exception) {
                    LOG.info("ExecutionException", exception);
                    super.setException(exception);
                    exchanger.setException(exception);
                }
            };
    
            FindNodeResponseHandler handler 
                = new FindNodeResponseHandler(context, randomId);
            start(handler, c);
        }
        
        private void handleBucketRefresh(FindNodeResult result) {
            // The number of Contacts from our RouteTable that didn't
            // respond. Random nodes that we discovered during the
            // lookup are not taken into account!
            routeTableFailureCount += result.getRouteTableFailureCount();
            
            if (routeTableFailureCount 
                    >= KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue()) {
                
                // Is it the first time? If so prune the RouteTable
                // and try again!
                if (!retriedToBootstrap) {
                    retriedToBootstrap = true;
                    handleStaleRouteTable();
                    
                // Did it happen again? If so give up!
                } else {
                    determinateIfBootstrapped();
                }
                
                // In any case exit here
                return;
            
            } else if (!foundNewContacts 
                    && !result.getPath().isEmpty()) {
                foundNewContacts = true;
            }
            
            if (randomIds.hasNext()) {
                refreshBucket(randomIds.next());
            } else {
                bootstrapped(true);
            }
        }
        
        private void handleStaleRouteTable() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Too many failures: " + routeTableFailureCount 
                        + ". Retrying to refresh all buckets.");
            }
            
            // The RouteTable is stale! Remove all non-alive Contacts,
            // rebuild the RouteTable and start over!
            context.getRouteTable().purge();
            
            // And Start over!
            findNearestNodes();
        }
        
        /**
         * Determinates whether or not we're bootstrapped.
         */
        private void determinateIfBootstrapped() {
            boolean bootstrapped = false;
            float alive = purgeAndGetPercenetage();
            
            // Check what percentage of the Contacts are alive
            if (alive >= BootstrapSettings.IS_BOOTSTRAPPED_RATIO.getValue()) {
                bootstrapped = true;
            }
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Bootstrapped: " + alive + " >= " 
                        + BootstrapSettings.IS_BOOTSTRAPPED_RATIO.getValue() 
                        + " -> " + bootstrapped);
            }
            
            bootstrapped(bootstrapped);
        }
        
        private float purgeAndGetPercenetage() {
            RouteTable routeTable = context.getRouteTable();
            synchronized (routeTable) {
                routeTable.purge();
                return RouteTableUtils.getPercentageOfAliveContacts(routeTable);
            }
        }
        
        private void bootstrapped(boolean bootstrapped) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Finishing bootstrapping: " + bootstrapped);
            }
            
            ResultType type = ResultType.BOOTSTRAP_FAILED;
            if (bootstrapped) {
                setBootstrapped(true);
                type = ResultType.BOOTSTRAP_SUCCEEDED;
            }
            
            long now = System.currentTimeMillis();
            long phaseOne = (startPhaseTwo - startPhaseOne);
            long phaseTwo = (now - startPhaseTwo);
            
            exchanger.setValue(new BootstrapResult(node, phaseOne, phaseTwo, type));
        }
        
        private <T> void start(DHTTask<T> task, OnewayExchanger<T, ExecutionException> c) {
            boolean doStart = false;
            synchronized (tasks) {
                if (!cancelled) {
                    tasks.add(task);
                    doStart = true;
                }
            }
            
            if (doStart) {
            	task.start(c);
            }
        }
       
        public void cancel() {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Canceling BootstrapProcess");
            }
            
            List<DHTTask<?>> copy = null;
            synchronized (tasks) {
                if (!cancelled) {
                    copy = new ArrayList<DHTTask<?>>(tasks);
                    tasks.clear();
                    cancelled = true;
                }
            }

            if (copy != null) {
                for (DHTTask<?> task : copy) {
                    task.cancel();
                }
            }
        }
    }
}
