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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.event.BootstrapEvent.Type;
import com.limegroup.mojito.event.exceptions.BootstrapTimeoutException;
import com.limegroup.mojito.event.exceptions.DHTException;
import com.limegroup.mojito.handler.response.BootstrapPingResponseHandler;
import com.limegroup.mojito.handler.response.FindNodeResponseHandler;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.util.BucketUtils;

/**
 * 
 */
public class BootstrapManager extends AbstractManager<BootstrapEvent> {
    
    private static final Log LOG = LogFactory.getLog(BootstrapManager.class);
    
	/**
	 * The maximum number of nodes that can fail per lookup (until the timeout)
	 * 
	 */
	private static final int MAX_NODE_FAILED_PER_LOOKUP = 
		(int)((KademliaSettings.NODE_LOOKUP_TIMEOUT.getValue()
				 / NetworkSettings.MAX_TIMEOUT.getValue())
				 * KademliaSettings.LOOKUP_PARAMETER.getValue()); 
    
    private Object lock = new Object();
    
    private BootstrapFuture future = null;
    
    private volatile boolean bootstrapped = false;
    
    public BootstrapManager(Context context) {
        super(context);
    }
    
    public boolean isBootstrapping() {
        synchronized(lock) {
            return future != null;
        }
    }
    
    public synchronized boolean isBootstrapped() {
        return bootstrapped;
    }
    
    public synchronized void setBootstrapped(boolean bootstrapped) {
        this.bootstrapped = bootstrapped;
    }
    
    public DHTFuture<BootstrapEvent> bootstrap() {
        synchronized(lock) {
        	if(future != null) {
            	future.cancel(true);
            }
            Bootstrapper bootstrapper = new Bootstrapper();
            future = new BootstrapFuture(bootstrapper);
            
            context.execute(future);
            
            return future;
        }
    }
    
    public DHTFuture<BootstrapEvent> bootstrap(Set<? extends SocketAddress> hostSet) {
        synchronized (lock) {
            if(future != null) {
            	future.cancel(true);
            }
            // Preserve the order of the Set
            Set<SocketAddress> copy = new LinkedHashSet<SocketAddress>(hostSet);
            Bootstrapper bootstrapper = new Bootstrapper(copy);
            future = new BootstrapFuture(bootstrapper);
            
            context.execute(future);
            
            return future;
        }
    }
    
    /**
     * Ping
     * Lookup own Node ID
     * Lookup radnom IDs
     */
    private class Bootstrapper implements Callable<BootstrapEvent> {
    	
        private Set<SocketAddress> hostSet = null;
        
        private long start = 0L;
        private long phaseOneStart = 0L;
        private long phaseTwoStart = 0L;
        private long stop = 0L;
        
        private boolean retriedBootstrap;
        
        private List<SocketAddress> failed = new ArrayList<SocketAddress>();
        
        private Bootstrapper() {
        }
        
        @SuppressWarnings("unchecked")
        private Bootstrapper(Set<? extends SocketAddress> hostSet) {
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
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Bootstrap failed: no bootstrap host");
                }
                return new BootstrapEvent(failed, System.currentTimeMillis()-start);
            }
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Bootstraping phase 1 from node: "+node);
            }
            
            future.fireResult(new BootstrapEvent(Type.PING_SUCCEEDED));
            phaseOneStart = System.currentTimeMillis();

            boolean foundNewContacts = startBootstrapLookups(node);
            
            stop = System.currentTimeMillis();
            
            long phaseZeroTime = phaseOneStart - start;
            long phaseOneTime = phaseTwoStart - phaseOneStart;
            long phaseTwoTime = stop - phaseTwoStart;
            
            if(!foundNewContacts) {
            	if(LOG.isDebugEnabled()) {
                    LOG.debug("Bootstrap failed: did not receive any response from phase 2!");
                }
                return new BootstrapEvent(failed, phaseOneTime+phaseTwoTime+phaseZeroTime);
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
            return new BootstrapEvent(failed, phaseZeroTime, phaseOneTime, phaseTwoTime, foundNewContacts);
        }
        
        private boolean startBootstrapLookups(Contact node) throws Exception{
        	
            phaseOne(node);
            
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
            
            BootstrapPingResponseHandler<SocketAddress> handler = 
                new BootstrapPingResponseHandler<SocketAddress>(context, hostSet);
            try {
                return handler.call();
            } catch (BootstrapTimeoutException exception) {
                failed.addAll(exception.getFailedHosts());
            }
            
            return null;
        }
        
        /**
         * Tries to ping the IPPs from the Route Table and returns the
         * first Contact that responds or null if none of them did respond
         */
        private Contact bootstrapFromRouteTable() throws Exception {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Bootstrapping from Route Table : "+context.getRouteTable());
            }
            
            Set<Contact> nodes = new TreeSet<Contact>(BucketUtils.MRS_COMPARATOR);
            nodes.addAll(context.getRouteTable().getLiveContacts());
            nodes.remove(context.getLocalNode());
            
            if(!nodes.isEmpty()) {
                BootstrapPingResponseHandler<Contact> handler = 
                    new BootstrapPingResponseHandler<Contact>(context, nodes);
                try {
                    return handler.call();
                } catch (BootstrapTimeoutException exception) {
                    failed.addAll(exception.getFailedHosts());
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
            return handler.call();
        }
        
        /**
         * Refresh all Buckets (Phase two)
         * 
         * When we detect that the routing table is stale, we purge it
         * and start the bootstrap all over again. 
         * A stale routing table can be detected by a high number of failures
         * during the lookup (alive hosts to expected result set size ratio).
         */
        private boolean phaseTwo(Contact node) throws Exception {
            boolean foundNewContacts = false;
            int failures = 0;
            
            List<KUID> randomId = context.getRouteTable().getRefreshIDs(true);
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
                    			< KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue()) {
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
                    	
                    } else if (!foundNewContacts) {
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
