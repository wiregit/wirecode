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
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.DHTException;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.handler.response.FindNodeResponseHandler;
import com.limegroup.mojito.handler.response.PingResponseHandler;
import com.limegroup.mojito.util.BucketUtils;

/**
 * 
 */
public class BootstrapManager extends AbstractManager {
    
    private static final Log LOG = LogFactory.getLog(BootstrapManager.class);
    
    private List<BootstrapListener> globalListeners = new ArrayList<BootstrapListener>();
    
    private Object lock = new Object();
    
    private BootstrapFuture future = null;
    
    public BootstrapManager(Context context) {
        super(context);
    }
    
    public void addBootstrapListener(BootstrapListener l) {
        synchronized (globalListeners) {
            globalListeners.add(l);
        }
    }
    
    public void removeBootstrapListener(BootstrapListener l) {
        synchronized (globalListeners) {
            globalListeners.remove(l);
        }
    }

    public BootstrapListener[] getBootstrapListeners() {
        synchronized (globalListeners) {
            return globalListeners.toArray(new BootstrapListener[0]);
        }
    }
    
    public boolean isBootstrapping() {
        synchronized(lock) {
            return future != null;
        }
    }
    
    public Future<BootstrapEvent> bootstrap(BootstrapListener l) {
        synchronized(lock) {
            if (future == null) {
                Bootstrapper bootstrapper = new Bootstrapper();
                future = new BootstrapFuture(bootstrapper);
                
                context.execute(future);
            }
            
            if (l != null) {
                future.addBootstrapListener(l);
            }
            
            return future;
        }
    }
    
    public Future<BootstrapEvent> bootstrap(List<? extends SocketAddress> hostList, BootstrapListener l) {
        synchronized (lock) {
            if (future == null) {
                Bootstrapper bootstrapper = new Bootstrapper(hostList);
                future = new BootstrapFuture(bootstrapper);
                
                context.execute(future);
            }
            
            if (l != null) {
                future.addBootstrapListener(l);
            }
            
            return future;
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
                LOG.debug("Bootstraping phase 1 from node: "+node);
            }
            
            phaseOneStart = System.currentTimeMillis();
            phaseOne(node);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Bootstraping phase 2 from node: "+node);
            }
            
            phaseTwoStart = System.currentTimeMillis();
            boolean foundNewContacts = phaseTwo(node);
            
            stop = System.currentTimeMillis();
            
            long phaseZeroTime = phaseOneStart - start;
            long phaseOneTime = phaseTwoStart - phaseOneStart;
            long phaseTwoTime = stop - phaseTwoStart;
            
            /*long totalTime = stop - start;
            StringBuilder buffer = new StringBuilder();
            buffer.append("foundNewNodes: ").append(foundNewNodes).append("\n");
            buffer.append("phaseZeroTime: ").append(phaseZeroTime).append("\n");
            buffer.append("phaseOneTime: ").append(phaseOneTime).append("\n");
            buffer.append("phaseTwoTime: ").append(phaseTwoTime).append("\n");
            buffer.append("totalTime: ").append(totalTime).append("\n");
            System.out.println(buffer.toString());*/
            
            return new BootstrapEvent(failed, phaseZeroTime, phaseOneTime, phaseTwoTime, foundNewContacts);
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
            LOG.debug("Bootstrapping from Route Table");
            
            List<Contact> nodes = BucketUtils.sort(context.getRouteTable().getLiveContacts());
            for (Contact node : nodes) {
                if (context.isLocalNode(node)) {
                    continue;
                }
                
                PingResponseHandler handler = new PingResponseHandler(context, node);
                try {
                    return handler.call();
                } catch (DHTException ignore) {
                    failed.add(node.getSocketAddress());
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
    
    private class BootstrapFuture extends FutureTask<BootstrapEvent> {
        
        private List<BootstrapListener> listeners = new ArrayList<BootstrapListener>();
        
        public BootstrapFuture(Callable<BootstrapEvent> task) {
            super(task);
        }

        public void addBootstrapListener(BootstrapListener l) {
            if (listeners == null) {
                listeners = new ArrayList<BootstrapListener>();
            }
            
            listeners.add(l);
        }
        
        @Override
        protected void done() {
            super.done();
            
            synchronized(lock) {
                future = null;
            }
            
            try {
                BootstrapEvent result = get();
                fireResult(result);
            } catch (Exception err) {
                fireException(err);
            }
        }
        
        private void fireResult(final BootstrapEvent result) {
            synchronized(globalListeners) {
                for (BootstrapListener l : globalListeners) {
                    l.handleResult(result);
                }
            }
            
            if (listeners != null) {
                for (BootstrapListener l : listeners) {
                    l.handleResult(result);
                }
            }
        }
        
        private void fireException(final Exception ex) {
            synchronized(globalListeners) {
                for (BootstrapListener l : globalListeners) {
                    l.handleException(ex);
                }
            }
            
            if (listeners != null) {
                for (BootstrapListener l : listeners) {
                    l.handleException(ex);
                }
            }
        }
    }
}
