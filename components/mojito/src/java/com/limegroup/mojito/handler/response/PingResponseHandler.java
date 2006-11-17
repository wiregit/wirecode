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
import java.math.BigInteger;
import java.net.SocketAddress;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.exceptions.DHTBackendException;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.exceptions.DHTBadResponseException;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.result.PingResult;
import com.limegroup.mojito.routing.Contact;

/**
 * The PingResponseHandler handles ping responses from Nodes
 * that we have pinged.
 */
public class PingResponseHandler extends AbstractResponseHandler<PingResult> {
    
    //private static final Log LOG = LogFactory.getLog(PingResponseHandler.class);
    
    private Contact sender;
    
    private KUID nodeId;
    
    private SocketAddress address;
    
    public PingResponseHandler(Context context, SocketAddress address) {
        this(context, null, null, address);
    }
    
    public PingResponseHandler(Context context, Contact contact) {
        this(context, null, contact.getNodeID(), contact.getContactAddress());
    }

    public PingResponseHandler(Context context, Contact sender, Contact contact) {
        this(context, sender, contact.getNodeID(), contact.getContactAddress());
    }
    
    public PingResponseHandler(Context context, KUID nodeId, SocketAddress address) {
        this(context, null, nodeId, address);
    }
    
    public PingResponseHandler(Context context, Contact sender, KUID nodeId, SocketAddress address) {
        super(context);
        
        this.sender = sender;
        this.nodeId = nodeId;
        this.address = address;
    }

    @Override
    protected void start() throws DHTException {
        super.start();
        
        PingRequest request = null;
        
        if (sender == null) {
            // Regular Ping
            request = context.getMessageHelper().createPingRequest(address);
        } else {
            // Node ID collision test Ping
            assert (sender.isFirewalled());
            request = context.getMessageFactory().createPingRequest(sender, MessageID.createWithSocketAddress(address));
        }
        
        try {
            context.getMessageDispatcher().send(nodeId, address, request, this);
        } catch (IOException err) {
            throw new DHTException(err);
        }
    }
    
    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        
        PingResponse response = (PingResponse)message;
        
        Contact node = response.getContact();
        SocketAddress externalAddress = response.getExternalAddress();
        BigInteger estimatedSize = response.getEstimatedSize();
        
        if (node.getContactAddress().equals(externalAddress)) {
            setException(new DHTBadResponseException(node + " is trying to set our external address to its address!"));
            return;
        }
        
        // Check if the other Node has the same ID as we do
        if (context.isLocalNodeID(node.getNodeID())) {
            
            // If so check if this was a Node ID collision
            // test ping. To do so see if we've set a customized
            // sender which has a different Node ID than our
            // actual Node ID
            
            if (sender == null) {
                setException(new DHTBadResponseException(node + " is trying to spoof our Node ID"));
            } else {
                setReturnValue(new PingResult(node, externalAddress, estimatedSize, time));
            }
            return;
        }
        
        context.setExternalAddress(externalAddress);
        context.addEstimatedRemoteSize(estimatedSize);
        
        setReturnValue(new PingResult(node, externalAddress, estimatedSize, time));
    }

    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        fireTimeoutException(nodeId, dst, message, time);
    }
    
    @Override
    protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
        setException(new DHTBackendException(nodeId, dst, message, e));
    }
}
