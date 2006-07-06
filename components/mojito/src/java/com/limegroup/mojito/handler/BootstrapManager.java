/*
 * Mojito Distributed Hash Tabe (DHT)
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.LookupListener;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;

/**
 * The BootstrapManager performs a lookup for the local Node ID
 * which is essentially the bootstrap process.
 */
public class BootstrapManager implements PingListener, LookupListener {
    
    private static final Log LOG = LogFactory.getLog(BootstrapManager.class);
    
    private Context context;

    private long startTime = 0L;
    
    private boolean phaseTwo = false;
    private boolean foundNewNodes = false;
    
    private BootstrapListener listener;
    
    private ArrayList<SocketAddress> failedHosts = new ArrayList<SocketAddress>(); 
    
    /**
     * List of Bucket IDs
     */
    private List<KUID> buckets = Collections.emptyList();
    
    /** 
     * An Iterator of SocketAddresses or ContactNodes depending on
     * the initialization.
     */
    private Iterator contacts;
    
    private int failures;
    
    private NetworkStatisticContainer networkStats;
    
    public BootstrapManager(Context context) {
        this.context = context;
        networkStats = context.getNetworkStats();
    }
    
    private long time() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Tries to bootstrap from the local Route Table.
     */
    public void bootstrap(BootstrapListener listener) throws IOException {
        this.listener = listener;
        this.contacts = context.getRouteTable().getAllNodesMRS().iterator();
        
        if (contacts.hasNext()) {
            startTime = System.currentTimeMillis();
            this.context.ping((ContactNode)contacts.next(), this);
        } else {
            fireNoBootstrapHost();
        }
    }
    
    /**
     * Tries to bootstrap from a List of Hosts.
     */
    public void bootstrap(List<? extends SocketAddress> hostList, 
            BootstrapListener listener) throws IOException {
        
        this.listener = listener;
        this.contacts = hostList.iterator();
        
        if (contacts.hasNext()) {
            startTime = System.currentTimeMillis();
            context.ping((SocketAddress)contacts.next(), this);
        } else {
            fireNoBootstrapHost();
        }
    }
    
    public void response(ResponseMessage response, long time) {
        if (response instanceof PingResponse) {
            pingSucceeded((PingResponse)response);
        }
    }

    /**
     * Initialized the lookup process after a successfull
     * ping request
     */
    private void pingSucceeded(PingResponse response) {
        try {
            context.lookup(context.getLocalNodeID(), this);
        } catch (IOException err) {
            LOG.error("Bootstrap lookup failed: ", err);
            
            firePhaseOneFinished();
            firePhaseTwoFinished();
        }
    }
    
    public void timeout(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time) {
        if (request instanceof PingRequest) {
            try {
                if (!pingFailed(address)) {
                    fireNoBootstrapHost();
                }
            } catch (IOException err) {
                LOG.error(err);
                fireNoBootstrapHost();
            }
        }
    }
    
    /**
     * Tries the next SocketAddress or ContactNode. Returns false
     * if we should give up.
     */
    private boolean pingFailed(SocketAddress address) throws IOException {
        networkStats.BOOTSTRAP_PING_FAILURES.incrementStat();
        if (LOG.isErrorEnabled()) {
            LOG.error("Initial bootstrap ping timeout, failure " + failures);
        }
        failedHosts.add(address);
        
        failures++;
        if (failures >= KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue()) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Initial bootstrap ping timeout, giving up bootstrap after " + failures + " tries");
            }
            
            return false;
        }
        
        // Possible switch from Iterator of SocketAddresses to an
        // Iterator of ContactNodes (but not vice versa).
        if (contacts == null || !contacts.hasNext()) {
            contacts = context.getRouteTable().getAllNodesMRS().iterator();
        }
        
        while(contacts.hasNext()) {
            Object obj = contacts.next();
            
            if (obj instanceof SocketAddress) {
                SocketAddress addr = (SocketAddress)obj;
                context.ping(addr, this);
                return true;
                
            } else if (obj instanceof ContactNode) {
                ContactNode node = (ContactNode)obj;
                //do not send to ourselve or the node which just timed out
                if (!node.getNodeID().equals(context.getLocalNodeID()) 
                        && !node.getSocketAddress().equals(address)) {
                    
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Retrying bootstrap ping with node: " + node);
                    }
                    
                    context.ping(node, this);
                    return true;
                }
            } else {
                
                if (LOG.isFatalEnabled()) {
                    if (obj != null) {
                        LOG.fatal("Expected either a SocketAddress or ContactNode but got: " + obj + "/" + obj.getClass());
                    } else {
                        LOG.fatal("Expected either a SocketAddress or ContactNode but got: " + obj);
                    }
                }
            }
        }
        
        return false;
    }
    
    public void found(KUID lookup, Collection c, long time) {
    }
    
    public void finish(KUID lookup, Collection c, long time) {
        if (!phaseTwo) {
            phaseTwo = true;
            firePhaseOneFinished();
            
            try {
                context.getRouteTable().refreshBuckets(true, this);
            } catch (IOException err) {
                LOG.error("Beginning second phase failed: ", err);
                firePhaseTwoFinished();
            }
        } else {
            if (!c.isEmpty()) {
                foundNewNodes = true;
            }
            
            buckets.remove(lookup);
            if (buckets.isEmpty()) {
                firePhaseTwoFinished();
            }
        }
    }

    public void setBuckets(List<KUID> buckets) {
        this.buckets = buckets;
        
        if (buckets.isEmpty()) {
            firePhaseTwoFinished();
        }
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
    
    private void firePhaseTwoFinished() {
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
}