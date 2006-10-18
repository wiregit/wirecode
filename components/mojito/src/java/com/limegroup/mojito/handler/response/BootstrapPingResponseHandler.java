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

package com.limegroup.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.exceptions.BootstrapTimeoutException;
import com.limegroup.mojito.exceptions.DHTBackendException;
import com.limegroup.mojito.exceptions.DHTBadResponseException;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.util.ContactUtils;

/**
 * This class pings a given number of hosts in parallel 
 * and returns the first successfull ping.
 */
public class BootstrapPingResponseHandler<V> extends AbstractResponseHandler<Contact> {
    
    private static final Log LOG = LogFactory.getLog(BootstrapPingResponseHandler.class);
    
    /** The number of pings to send in parallel. */
    private int parallelism = KademliaSettings.PARALLEL_PINGS.getValue();
    
    /** The list of hosts to ping */
    private Set<V> hostsToPing;
    
    /** The list of hosts that have failed (mutually exclusive with hostsToPing) */
    private Set<SocketAddress> failedHosts = new HashSet<SocketAddress>();
    
    private final Context context;
    
    /** The number of active pings */
    private int activePings = 0;
    
    public BootstrapPingResponseHandler(Context context, Set<V> hosts) {
        super(context);
        this.context = context;
        this.hostsToPing = hosts;
    }
    
    @Override
    protected synchronized void start() throws DHTException {
        super.start();
        
        try {
            sendPings();
        } catch (IOException err) {
            throw new DHTException(err);
        }
    }

    private void sendPings() throws IOException {
        for (Iterator<V> iter = hostsToPing.iterator(); 
                iter.hasNext() && (activePings < parallelism); ) {
            
            V host = iter.next();
            iter.remove();
            
            if (host instanceof Contact) {
                Contact contact = (Contact) host;
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Sending bootstrap ping to contact: " + contact);
                }
                
                PingRequest request = context.getMessageHelper()
                    .createPingRequest(contact.getContactAddress());
                
                context.getMessageDispatcher().send(contact, request, this);
                
            } else { //it has to be a SocketAddress
                
                SocketAddress address = (SocketAddress) host;
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Sending bootstrap ping to address: " + address);
                }
                
                PingRequest request = context.getMessageHelper()
                    .createPingRequest(address);
                context.getMessageDispatcher().send(address, request, this);
            }
            
            activePings++;
        }
        
        if (activePings == 0) {
            setException(new DHTException("All SocketAddresses were invalid and there are no Hosts left to Ping"));
        }
    }
    
    private boolean shouldStop() {
        return (hostsToPing.isEmpty()
                || (failedHosts.size() 
                        >= KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue()));
    }

    @Override
    public synchronized void handleResponse(ResponseMessage response, long time) throws IOException {
        // Synchronizing handleResponse() so that response()
        // doesn't get called if this Handler isDone or isCancelled
        super.handleResponse(response, time);
    }

    @Override
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        PingResponse response = (PingResponse)message;
        SocketAddress externalAddress = response.getExternalAddress();
        
        Contact node = response.getContact();
        if (node.getContactAddress().equals(externalAddress)) {
            if (hostsToPing.isEmpty()) {
                setException(new DHTBadResponseException(node 
                                + " is trying to set our external address to its address!"));
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(node + " is trying to set our external address to its address!");
                }
                
                sendPings();
            }
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Got bootstrap ping response from " + message.getContact());
        }
        
        context.setExternalAddress(externalAddress);
        context.addEstimatedRemoteSize(response.getEstimatedSize());
        
        setReturnValue(message.getContact());
    }

    @Override
    public synchronized void handleError(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        // Synchronizing handleError() so that error()
        // doesn't get called if this Handler isDone or isCancelled
        super.handleError(nodeId, dst, message, e);
    }
    
    @Override
    protected synchronized void error(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        
        if(LOG.isErrorEnabled()) {
            LOG.error("Bootstrap error", e);
        }
        
        if(e instanceof SocketException && !shouldStop()) {
            try {
                timeout(nodeId, dst, message, -1L);
            } catch (IOException err) {
                LOG.error("IOException", err);
                
                if (hostsToPing.isEmpty()) {
                    setException(new DHTException(err));
                }
            }
        } else {
            setException(new DHTBackendException(nodeId, dst, message, e));
        }
    }
    
    @Override
    public synchronized void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage request, long time) throws IOException {
        // Synchronizing handleTimeout() so that timeout()
        // doesn't get called if this Handler isDone or isCancelled
        super.handleTimeout(nodeId, dst, request, time);
    }
    
    @Override
    protected synchronized void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Timeout on bootstrap ping to " + ContactUtils.toString(nodeId, dst));
        }
        
        // this is to make sure hostsToPing is a true Set (i.e. contains no publicates)
        assert (!failedHosts.contains(dst)); 
        failedHosts.add(dst);
        activePings--;
        
        if(shouldStop()) {
            setException(new BootstrapTimeoutException(failedHosts));
            return;
        }
        
        sendPings();
    }
}