/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.handler.request.PingRequestHandler;
import de.kapsi.net.kademlia.io.Receipt;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.response.PingResponse;
import de.kapsi.net.kademlia.settings.NetworkSettings;

public class PingResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(PingRequestHandler.class);
    
    private PingListener l;
    
    private final NetworkStatisticContainer networkStats;
    
    public PingResponseHandler(Context context, PingListener l) {
        super(context);
        networkStats = context.getNetworkStats();
        this.l = l;
    }

    public void handleResponse(Receipt receipt, final KUID nodeId, 
            final SocketAddress src, Message message, final long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + ContactNode.toString(nodeId, src) 
                    + " succeeded");
        }
        
        networkStats.PINGS_OK.incrementStat();
        
        PingResponse response = (PingResponse)message;
        SocketAddress externalAddress = response.getSocketAddress();
        
        if (externalAddress != null) {
            InetAddress extAddr = ((InetSocketAddress)externalAddress).getAddress();
            InetAddress srcAddr = ((InetSocketAddress)src).getAddress();
            
            if (!extAddr.equals(srcAddr) || NetworkSettings.ALLOW_MULTIPLE_NODES.getValue()) {
                
                SocketAddress currentAddress = context.getExternalSocketAddress();
                if (!externalAddress.equals(currentAddress)) {
                    
                    ExternalAddressVerifier handler 
                        = new ExternalAddressVerifier(context, nodeId, src, externalAddress);
                    
                    handler.pingRandomContactNode();
                }
            }
        }
        
        context.addEstimatedRemoteSize(response.getEstimatedSize());
        
        if (l != null) {
            getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.pingSuccess(nodeId, src, time);
                }
            });
        }
    }
    
    public void handleTimeout(Receipt receipt, final KUID nodeId, 
            final SocketAddress dst, long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + ContactNode.toString(nodeId, dst) + " failed");
        }
        
        networkStats.PINGS_FAILED.incrementStat();
        
        if (l != null) {
            getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.pingTimeout(nodeId, dst);
                }
            });
        }
    }
    
    private class ExternalAddressVerifier extends AbstractResponseHandler {
        
        private boolean done = false;
        private int errors = 0;
        
        private KUID nodeId;
        private InetAddress addr;
        
        private SocketAddress externalAddress;
        
        private ExternalAddressVerifier(Context context, 
                KUID nodeId, SocketAddress src, SocketAddress externalAddress) {
            super(context);
            
            this.nodeId = nodeId;
            this.addr = ((InetSocketAddress)src).getAddress();
            
            this.externalAddress = externalAddress;
        }

        private void pingRandomContactNode() throws IOException {
            ContactNode node = getRandomContactNode();
            if (node == null) {
                return;
            }
            node.setPinged(true);
            PingRequest request = context.getMessageFactory().createPingRequest();
            context.getMessageDispatcher().send(node, request, this);
        }
        
        private ContactNode getRandomContactNode() {
            List nodes = context.getRouteTable().select(KUID.createRandomNodeID(), 10, true, false);
            
            for(Iterator it = nodes.iterator(); it.hasNext(); ) {
                ContactNode node = (ContactNode)it.next();
                KUID nodeId = node.getNodeID();
                InetAddress addr = ((InetSocketAddress)node.getSocketAddress()).getAddress();
                
                // Make sure we don't ping:
                // - Ourself since we don't know our external address
                // - The same Node (ID) that told us the external address
                // - The same Node (IP) that told us the external address
                // - A node that we are allready pinging
                if (!nodeId.equals(context.getLocalNodeID())
                        && !nodeId.equals(this.nodeId)
                        && !addr.equals(this.addr)
                        && !node.isPinged()) {
                    return node;
                }
            }
            
            return null;
        }
        
        public void handleResponse(Receipt receipt, KUID nodeId, 
                    SocketAddress src, Message message, long time) throws IOException {
            
            networkStats.PINGS_OK.incrementStat();
            
            if (done) {
                return;
            }
            
            done = true;
            
            PingResponse response = (PingResponse)message;
            SocketAddress externalAddress = response.getSocketAddress();
            
            if (externalAddress != null 
                    && externalAddress.equals(this.externalAddress)) {
                context.setExternalSocketAddress(externalAddress);
            }
        }

        public void handleTimeout(Receipt receipt, KUID nodeId, SocketAddress dst, long time) throws IOException {
            
            networkStats.PINGS_FAILED.incrementStat();
            
            if (done) {
                return;
            }
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(ContactNode.toString(nodeId, dst) + " did not respond");
            }
            
            if (++errors >= NetworkSettings.MAX_ERRORS.getValue()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Max number of errors occured. Giving up!");
                }
                return;
            }
            
            pingRandomContactNode();
        }
    }
}
