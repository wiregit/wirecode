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
 
package com.limegroup.mojito.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.LookupListener;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;
import com.limegroup.mojito.util.BucketUtils;

/**
 * The BootstrapManager performs a lookup for the local Node ID
 * which is essentially the bootstrap process.
 */
public class BootstrapManager {
    
    private static final Log LOG = LogFactory.getLog(BootstrapManager.class);
    
    protected final Context context;
    
    private BootstrapListener listener;
    
    private long startTime = -1L;
    
    private List<SocketAddress> failedHosts = new ArrayList<SocketAddress>();
    
    private NetworkStatisticContainer networkStats;
    
    public BootstrapManager(Context context) {
        this.context = context;
        networkStats = context.getNetworkStats();
    }
    
    public void bootstrap(BootstrapListener listener) {
        this.listener = listener;
        
        startTime = System.currentTimeMillis();
        (new RouteTablePhaseZero()).start();
    }
    
    public void bootstrap(List<? extends SocketAddress> hostList, 
            BootstrapListener listener) {
        this.listener = listener;
        
        startTime = System.currentTimeMillis();
        (new HostListPhaseZero(hostList)).start();
    }
    
    public long time() {
        if (startTime < 0L) {
            return -1L;
        }
        
        return System.currentTimeMillis() - startTime;
    }
    
    private void firePhaseOneFinished() {
        if (listener != null) {
            final long time = time();
            
            context.fireEvent(new Runnable() {
                public void run() {
                    listener.phaseOneComplete(time);
                }
            });
        }
    }
    
    private void firePhaseTwoFinished(final boolean foundNewNodes) {
        context.setBootstrapping(false);
        
        if (listener != null) {
            final long time = time();
            
            context.fireEvent(new Runnable() {
                public void run() {
                    listener.phaseTwoComplete(foundNewNodes, time);
                }
            });
        }
    }
    
    private void fireNoBootstrapHost() {
        context.setBootstrapping(false);
        if (listener != null) {
            context.fireEvent(new Runnable() {
                public void run() {
                    listener.noBootstrapHost(failedHosts);
                }
            });
        }
    }
    
    private abstract class PhaseZero implements PingListener {
        
        public void start() {
            next();
        }
        
        public void response(ResponseMessage response, long time) {
            try {
                PhaseOne phaseOne = new PhaseOne();
                context.lookup(context.getLocalNodeID(), phaseOne);
            } catch (IOException err) {
                LOG.error("Bootstrap lookup failed: ", err);
                
                firePhaseOneFinished();
                firePhaseTwoFinished(false);
            }
        }
        
        public void timeout(KUID nodeId, SocketAddress address, 
                RequestMessage request, long time) {
            
            failedHosts.add(address);
            
            networkStats.BOOTSTRAP_PING_FAILURES.incrementStat();
            if (LOG.isErrorEnabled()) {
                LOG.error("Initial bootstrap ping timeout, failure " + failedHosts.size());
            }
            
            if (failedHosts.size() < KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue()) {
                next();
                return;
            }
            
            if (LOG.isErrorEnabled()) {
                LOG.error("Initial bootstrap ping timeout, giving up bootstrap after " + failedHosts.size() + " tries");
            }
            
            fireNoBootstrapHost();
        }
        
        private void next() {
            try {
                if (!pingNextFromList()) {
                    fireNoBootstrapHost();
                }
            } catch (IOException err) {
                LOG.error("IOException", err);
                fireNoBootstrapHost();
            }
        }
        
        protected abstract boolean pingNextFromList() throws IOException;
    }
    
    private class HostListPhaseZero extends PhaseZero {
        
        private int index = 0;
        private List<? extends SocketAddress> hostList;
        
        private HostListPhaseZero(List<? extends SocketAddress> hostList) {
            this.hostList = hostList;
        }
        
        protected boolean pingNextFromList() throws IOException {
            if (index < hostList.size()) {
                SocketAddress address = hostList.get(index++);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Trying to bootstrap from " + address);
                }
                
                context.ping(address, this);
                return true;
            }
            
            
            // Try the entries in the RouteTable
            (new RouteTablePhaseZero()).start();
            return true;
        }
    }
    
    private class RouteTablePhaseZero extends PhaseZero {
        
        private int index = 0;
        private List<Contact> contacts;
        
        public RouteTablePhaseZero() {
            contacts = BucketUtils.sort(context.getRouteTable().getLiveContacts());
        }

        protected boolean pingNextFromList() throws IOException {
            if (index < contacts.size()) {
                Contact node = contacts.get(index++);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Trying to bootstrap from " + node);
                }
                
                context.ping(node, this);
                return true;
            }
            
            return false;
        }
    }

    private class PhaseOne implements LookupListener {

        public void response(ResponseMessage response, long time) {
        }

        public void timeout(KUID nodeId, SocketAddress address, 
                RequestMessage request, long time) {
        }
        
        public void found(KUID lookup, Collection c, long time) {
        }
        
        public void finish(KUID lookup, Collection c, long time) {
            firePhaseOneFinished();
            
            List<KUID> ids = context.getRouteTable().getRefreshIDs(true);
            if (ids.isEmpty()) {
                firePhaseTwoFinished(false);
                return;
            }
            
            try {
                PhaseTwo phaseTwo = new PhaseTwo(ids);
                for(KUID nodeId : ids) {
                    context.lookup(nodeId, phaseTwo);
                }
            } catch (IOException err) {
                LOG.error("Beginning second phase failed: ", err);
                firePhaseTwoFinished(false);
            }
        }
    }
    
    private class PhaseTwo implements LookupListener {
        
        private List<KUID> ids;
        
        private boolean foundNewNodes = false;
        
        private PhaseTwo(List<KUID> ids) {
            this.ids = ids;
        }
        
        public void response(ResponseMessage response, long time) {
        }

        public void timeout(KUID nodeId, SocketAddress address, 
                RequestMessage request, long time) {
        }
        
        public void found(KUID lookup, Collection c, long time) {
        }
        
        public void finish(KUID lookup, Collection c, long time) {
            if (!c.isEmpty()) {
                foundNewNodes = true;
            }
            
            boolean removed = ids.remove(lookup);
            assert (removed == true);
            
            if (ids.isEmpty()) {
                firePhaseTwoFinished(foundNewNodes);
            }
        }
    }
}
