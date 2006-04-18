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
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.handler.request.PingRequestHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.response.PingResponse;
import de.kapsi.net.kademlia.security.CryptoHelper;
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

    public void handleResponse(final KUID nodeId, final SocketAddress src, 
            Message message, final long time) throws IOException {
        
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
                    
                    PingRequest request = context.getMessageFactory().createPingRequest(PingRequest.SIGN);
                    ResponseHandler handler = new VerifyExternalAddressHandler(context, externalAddress);
                    context.getMessageDispatcher().send(context.getLocalNodeID(), externalAddress, request, handler);
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
    
    public void handleTimeout(final KUID nodeId, final SocketAddress dst, 
            long time) throws IOException {
        
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
    
    private static class VerifyExternalAddressHandler extends AbstractResponseHandler {
        
        private SocketAddress externalAddress;
        
        private VerifyExternalAddressHandler(Context context, 
                SocketAddress externalAddress) {
            super(context);
            
            this.externalAddress = externalAddress;
        }

        public void handleResponse(KUID nodeId, SocketAddress src, 
                    Message message, long time) throws IOException {
            
            KUID messageId = message.getMessageID();
            byte[] signature = message.getSignature();
            
            try {
                if (CryptoHelper.verify(context.getPublicKey(), messageId.getBytes(), signature)) {
                    // TODO externalAddress and src should be equal 
                    // until the Router is doing some weird stuff!?
                    
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Setting external address from " 
                                + context.getExternalSocketAddress() 
                                + " to " + externalAddress);
                    }
                    context.setExternalSocketAddress(externalAddress);
                } else {
                    if (LOG.isErrorEnabled()) {
                        LOG.error(externalAddress + " is not my external address!");
                    }
                }
            } catch (InvalidKeyException e) {
                LOG.error("PingResponseHandler invalid key error: ",e);
            } catch (SignatureException e) {
                LOG.error("PingResponseHandler signature error: ",e);
            }
        }

        public void handleTimeout(KUID nodeId, SocketAddress dst, long time) throws IOException {
            // No special handling of errors required as we sent a
            // ping on the local network which shouldn't fail due to
            // network errors like packet loss. Keep in mind that this 
            // method gets also called if destination and response 
            // Node ID didn't match. That means the guy sent us an
            // IP:Port of an another Lime DHT Node. See Receipt!
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(ContactNode.toString(nodeId, dst) + " did not respond");
            }
        }
    }
}
