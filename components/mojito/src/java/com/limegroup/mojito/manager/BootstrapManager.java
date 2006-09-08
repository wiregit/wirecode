/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
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

package com.limegroup.mojito.manager;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.exceptions.BootstrapTimeoutException;
import com.limegroup.mojito.exceptions.CollisionException;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.handler.response.BootstrapPingResponseHandler;
import com.limegroup.mojito.handler.response.FindNodeResponseHandler;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.util.BucketUtils;

/**
 * The BootstrapManager manages the entire bootstrap process.
 */
public class BootstrapManager extends AbstractManager<BootstrapEvent> {
    
    private static final Log LOG = LogFactory.getLog(BootstrapManager.class);
    
    /**
     * The maximum number of nodes that can fail per lookup (until the timeout)
     */
    private static final int MAX_NODE_FAILED_PER_LOOKUP 
        = (int)((KademliaSettings.FIND_NODE_LOOKUP_TIMEOUT.getValue()
                / NetworkSettings.TIMEOUT.getValue())
                    * KademliaSettings.FIND_NODE_PARALLEL_LOOKUPS.getValue()); 
    
    private Object lock = new Object();
    
    private BootstrapFuture future = null;
    
    private volatile boolean bootstrapped = false;
    
    public BootstrapManager(Context context) {
        super(context);
    }
    
    /**
     * Returns true if this Node is currently bootstrapping
     */
    public boolean isBootstrapping() {
        synchronized(lock) {
            return future != null;
        }
    }
    
    /**
     * Returns true if this Node has bootstrapped successfully
     */
    public boolean isBootstrapped() {
        return bootstrapped;
    }
    
    /**
     * An internal method to set the bootsrapped flag.
     * Meant for internal use only.
     */
    public void setBootstrapped(boolean bootstrapped) {
        this.bootstrapped = bootstrapped;
    }
    
    /**
     * Re-Bootstrap from the internal RouteTable
     */
    @SuppressWarnings("unchecked")
    public DHTFuture<BootstrapEvent> bootstrap() {
        return bootstrap(Collections.EMPTY_SET);
    }
    
    /**
     * Bootstrap from the given Set of Hosts
     */
    public DHTFuture<BootstrapEvent> bootstrap(Set<? extends SocketAddress> hostSet) {
        synchronized (lock) {
            // Cancel an active process
            if(future != null) {
            	future.cancel(true);
                future = null;
            }
            
            // Preserve the order of the Set
            Set<SocketAddress> copy = new LinkedHashSet<SocketAddress>(hostSet);
            BootstrapProcess process = new BootstrapProcess(copy);
            future = new BootstrapFuture(process);
            context.execute(future);
            return future;
        }
    }
    
    /**
     * The bootstrap process looks like:
     * 
     * +--->
     * |   1) Find a Node that responds to our Ping
     * |   2) Lookup own Node ID
     * |   3) Lookup radnom IDs
     * +---4) Prune RouteTable and restart if too many errors in #3
     *     5) Done
     */
    private class BootstrapProcess implements Callable<BootstrapEvent> {
    	
        private Set<SocketAddress> hostSet = null;
        
        private long start = 0L;
        private long phaseOneStart = 0L;
        private long phaseTwoStart = 0L;
        private long stop = 0L;
        
        private boolean retriedBootstrap = false;
        
        private List<SocketAddress> failedHosts = new ArrayList<SocketAddress>();

        @SuppressWarnings("unchecked")
        private BootstrapProcess(Set<? extends SocketAddress> hostSet) {
            this.hostSet = (Set<SocketAddress>)hostSet;
        }
        
        public BootstrapEvent call() throws Exception {
            start = System.currentTimeMillis();
            Contact node = null;
            if (hostSet != null && !hostSet.isEmpty()) {
                node = bootstrapFromHostSet();
            } else {
                node = bootstrapFromRouteTable();
            }
            
            if (node == null) {
                LOG.debug("Bootstrap failed: no bootstrap host");
                return BootstrapEvent.createBootstrappingFailedEvent(
                        failedHosts, System.currentTimeMillis()-start);
            }
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Bootstraping phase 1 from node: " + node);
            }
            
            future.fireResult(BootstrapEvent.createBootstrapPingSucceededEvent());
            phaseOneStart = System.currentTimeMillis();

            boolean foundNewContacts = startBootstrapLookups(node);
            
            stop = System.currentTimeMillis();
            
            long phaseZeroTime = phaseOneStart - start;
            long phaseOneTime = phaseTwoStart - phaseOneStart;
            long phaseTwoTime = stop - phaseTwoStart;
            
            if(!foundNewContacts) {
            	if(LOG.isDebugEnabled()) {
                    LOG.debug("Bootstrap failed at phase 1 or phase 2");
                }
                return BootstrapEvent.createBootstrappingFailedEvent(
                        failedHosts, phaseOneTime+phaseTwoTime+phaseZeroTime);
            }
            
            /*long totalTime = stop - start;
            StringBuilder buffer = new StringBuilder();
            buffer.append("foundNewNodes: ").append(foundNewNodes).append("\n");
            buffer.append("phaseZeroTime: ").append(phaseZeroTime).append("\n");
            buffer.append("phaseOneTime: ").append(phaseOneTime).append("\n");
            buffer.append("phaseTwoTime: ").append(phaseTwoTime).append("\n");
            buffer.append("totalTime: ").append(totalTime).append("\n");
            System.out.println(buffer.toString());*/
            
            bootstrapped = true;
            return BootstrapEvent.createBootstrappingSucceededEvent(
                    failedHosts, phaseZeroTime, phaseOneTime, phaseTwoTime, foundNewContacts);
        }
        
        /**
         * Starts phase 1 (local ID lookup) and phase 2 (random ids lookup) of the 
         * bootstrap process.
         * 
         * @param node The node to bootstrap from
         * @return true if phase 1 and phase 2 succeeded, false in case of failure
         * @throws Exception
         */
        private boolean startBootstrapLookups(Contact node) throws Exception {
            
            while(true) {
                try {
                    FindNodeEvent findNode = phaseOne(node);
                    if(findNode.getNodes().isEmpty()) {
                        failedHosts.add(node.getContactAddress());
                        return false;
                    }
                    break;
                } catch (CollisionException err) {
                    LOG.error("CollisionException", err);
                    handleCollision(err.getCollideContact());
                }
            }
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Bootstraping phase 2 from node: "+node);
            }
            
            phaseTwoStart = System.currentTimeMillis();
            return phaseTwo(node);
        }
        
        /**
         * Tries to ping the IPPs from the hostList and returns the first
         * Contact that responds or null if none of them did respond
         */
        private Contact bootstrapFromHostSet() throws Exception {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Bootstrapping from host Set: "+hostSet);
            }
            
            BootstrapPingResponseHandler<SocketAddress> handler 
                = new BootstrapPingResponseHandler<SocketAddress>(context, hostSet);
            try {
                return handler.call();
            } catch (BootstrapTimeoutException exception) {
                failedHosts.addAll(exception.getFailedHosts());
            }
            
            return null;
        }
        
        /**
         * Tries to ping the IPPs from the Route Table and returns the
         * first Contact that responds or null if none of them did respond
         */
        private Contact bootstrapFromRouteTable() throws Exception {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Bootstrapping from RouteTable : " + context.getRouteTable());
            }
            
            Set<Contact> nodes = new HashSet<Contact>();
            List<Contact> contactList = context.getRouteTable().getLiveContacts();
            Collections.sort(contactList, BucketUtils.MRS_COMPARATOR);
            nodes.addAll(contactList);
            nodes.remove(context.getLocalNode());
            
            if(!nodes.isEmpty()) {
                BootstrapPingResponseHandler<Contact> handler 
                    = new BootstrapPingResponseHandler<Contact>(context, nodes);
                
                try {
                    return handler.call();
                } catch (BootstrapTimeoutException exception) {
                    failedHosts.addAll(exception.getFailedHosts());
                }
            }
            
            return null;
        }
        
        /**
         * Do a lookup for myself (Phase one)
         */
        private FindNodeEvent phaseOne(Contact node) throws Exception {
            
            FindNodeResponseHandler handler 
                = new FindNodeResponseHandler(context, node, context.getLocalNodeID());
            FindNodeEvent evt = handler.call();
            
            // Ping all Contacts that have our Node ID. If any
            // of them responds then change our Node ID and
            // try again!
            for (Contact c : evt.getCollisions()) {
                try {
                    Contact collidesWith = context.collisionPing(c).get();
                    throw new CollisionException(collidesWith, 
                        context.getLocalNode() + " collides with " + collidesWith); 
                } catch (Exception err) {
                    if (err instanceof CollisionException) {
                        throw err;
                    }
                    
                    LOG.error("Exception", err);
                }
            }
            
            return evt;
        }
        
        private void handleCollision(Contact collide) {
            context.changeNodeID();
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
         * 
         */
        private boolean phaseTwo(Contact node) throws Exception {
            
            boolean foundNewContacts = false;
            int failures = 0;
            
            List<KUID> randomId = context.getRouteTable().getRefreshIDs(true);
            if(randomId.isEmpty()) {
                return true;
            }
            
            for (KUID nodeId : randomId) {
                FindNodeResponseHandler handler 
                    = new FindNodeResponseHandler(context, nodeId);
                try {
                    FindNodeEvent evt = handler.call();
                    
                    float responseRatio = 
                    	evt.getNodes().size()/(float)KademliaSettings.REPLICATION_PARAMETER.getValue();
                    
                    if(responseRatio < KademliaSettings.BOOTSTRAP_RATIO.getValue()) {
                    	failures++;
                    	
                    	if(LOG.isDebugEnabled()) {
                    	    LOG.debug("Bootstrap poor lookup ratio. Failures: " +failures);
                    	}
                    	
                    	if((failures * MAX_NODE_FAILED_PER_LOOKUP) 
                                >= KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue()) {
                            //routing table is stale! remove unknown and dead contacts
                            //and start over
                            context.getRouteTable().purge();
                            
                            if(!retriedBootstrap) {
                                LOG.debug("Retrying bootstrap from phase 2");
                                retriedBootstrap = true;
                                return startBootstrapLookups(node);
                            } else {
                                return false;
                            }
                    	}
                    	
                    } else if (!foundNewContacts && !evt.getNodes().isEmpty()) {
                        foundNewContacts = true;
                    }
                } catch (DHTException ignore) {
                    failures++;
                }
            }
            return foundNewContacts;
        }
    }
    
    private class BootstrapFuture extends AbstractDHTFuture {
        
        public BootstrapFuture(Callable<BootstrapEvent> task) {
            super(task);
        }
        
        @Override
        protected void deregister() {
            synchronized(lock) {
                future = null;
            }
        }
    }
}