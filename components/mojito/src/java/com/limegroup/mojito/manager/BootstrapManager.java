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
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.DHTException;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.event.BootstrapEvent.Type;
import com.limegroup.mojito.exceptions.CollisionException;
import com.limegroup.mojito.handler.response.FindNodeResponseHandler;
import com.limegroup.mojito.handler.response.PingResponseHandler;
import com.limegroup.mojito.util.BucketUtils;

/**
 * 
 */
public class BootstrapManager extends AbstractManager<BootstrapEvent> {
    
    private static final Log LOG = LogFactory.getLog(BootstrapManager.class);
    
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
            if (future == null) {
                Bootstrapper bootstrapper = new Bootstrapper();
                future = new BootstrapFuture(bootstrapper);
                
                context.execute(future);
            }
            
            return future;
        }
    }
    
    public DHTFuture<BootstrapEvent> bootstrap(List<? extends SocketAddress> hostList) {
        synchronized (lock) {
            if (future == null) {
                Bootstrapper bootstrapper = new Bootstrapper(
                        new ArrayList<SocketAddress>(hostList));
                future = new BootstrapFuture(bootstrapper);
                
                context.execute(future);
            }
            
            return future;
        }
    }
    
    /**
     * Cancels a currently active bootstrap process
     */
    @Deprecated // Can be removed as solved differently on mojito-v3-branch
    public boolean cancelBootstrapping() {
        synchronized (lock) {
            if (future != null) {
                future.cancel(true);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Ping
     * Lookup own Node ID
     * Lookup radnom IDs
     */
    private class Bootstrapper implements Callable<BootstrapEvent> {
        
        private List<SocketAddress> hostList = null;
        
        private long start = 0L;
        private long phaseOneStart = 0L;
        private long phaseTwoStart = 0L;
        private long stop = 0L;
        
        private List<SocketAddress> failed = new ArrayList<SocketAddress>();
        
        private Bootstrapper() {
        }
        
        @SuppressWarnings("unchecked")
        private Bootstrapper(List<? extends SocketAddress> hostList) {
            this.hostList = (List<SocketAddress>)hostList;
        }
        
        public BootstrapEvent call() throws Exception {
            start = System.currentTimeMillis();
            Contact node = null;
            if (hostList != null && !hostList.isEmpty()) {
                node = bootstrapFromHostList();
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
                LOG.debug("Bootstraping phase 1 from node: " + node);
            }
            
            future.fireResult(new BootstrapEvent(Type.PING_SUCCEEDED));
            phaseOneStart = System.currentTimeMillis();
            
            while(true) {
                try {
                    phaseOne(node);
                    break;
                } catch (CollisionException err) {
                    LOG.error("CollisionException", err);
                    handleCollision(err.getCollideContact());
                }
            }
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Bootstraping phase 2 from node: " + node);
            }
            
            phaseTwoStart = System.currentTimeMillis();
            boolean foundNewContacts = phaseTwo(node);
            
            stop = System.currentTimeMillis();
            
            long phaseZeroTime = phaseOneStart - start;
            long phaseOneTime = phaseTwoStart - phaseOneStart;
            long phaseTwoTime = stop - phaseTwoStart;
            
            bootstrapped = true;
            return new BootstrapEvent(failed, phaseZeroTime, phaseOneTime, phaseTwoTime, foundNewContacts);
        }
        
        private void handleCollision(Contact collide) {
            context.changeNodeID();
        }
        
        /**
         * Tries to ping the IPPs from the hostList and returns the first
         * Contact that responds or null if none of them did respond
         */
        private Contact bootstrapFromHostList() throws Exception {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Bootstrapping from host list: "+hostList);
            }
            
            for (SocketAddress address : hostList) {
                PingResponseHandler handler = new PingResponseHandler(context, address);
                try {
                    return handler.call();
                } catch (DHTException ignore) {
                    failed.add(address);
                }
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
            
            List<Contact> nodes = BucketUtils.sort(context.getRouteTable().getLiveContacts());
            for (Contact node : nodes) {
                if (context.isLocalNode(node)) {
                    continue;
                }
                
                PingResponseHandler handler = new PingResponseHandler(context, node);
                try {
                    return handler.call();
                } catch (DHTException ignore) {
                    failed.add(node.getContactAddress());
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
            handler.setCollisionCheckEnabled(true);
            return handler.call();
        }
        
        /**
         * Refresh all Buckets (Phase two)
         */
        private boolean phaseTwo(Contact node) throws Exception {
            boolean foundNewContacts = false;
            List<KUID> randomId = context.getRouteTable().getRefreshIDs(true);
            for (KUID nodeId : randomId) {
                FindNodeResponseHandler handler 
                    = new FindNodeResponseHandler(context, nodeId);
                try {
                    FindNodeEvent evt = handler.call();
                    if (!foundNewContacts && !evt.getNodes().isEmpty()) {
                        foundNewContacts = true;
                    }
                } catch (DHTException ignore) {}
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
