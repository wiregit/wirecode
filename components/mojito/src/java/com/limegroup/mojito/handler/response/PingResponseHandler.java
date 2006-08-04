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

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.DHTException;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;

/**
 * The PingResponseHandler handles ping responses from Nodes
 * that we have pinged.
 */
public class PingResponseHandler extends AbstractResponseHandler<Contact> {
    
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
    protected void start() throws Exception {
        PingRequest request = null;
        
        if (sender == null) {
            request = context.getMessageHelper().createPingRequest(address);
        } else {
            request = context.getMessageFactory().createPingRequest(sender, MessageID.create(address));
        }
        
        context.getMessageDispatcher().send(nodeId, address, request, this);
    }
    
    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        
        PingResponse response = (PingResponse)message;
        SocketAddress externalAddress = response.getExternalAddress();
        
        Contact node = response.getContact();
        if (node.getContactAddress().equals(externalAddress)) {
            setException(new Exception(node + " is trying to set our external address to its address!"));
            return;
        }
        
        context.setExternalSocketAddress(externalAddress);
        context.addEstimatedRemoteSize(response.getEstimatedSize());
        
        setReturnValue(message.getContact());
    }

    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        fireTimeoutException(nodeId, dst, message, time);
    }
    
    @Override
    public void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        setException(new DHTException(nodeId, dst, message, -1L, e));
    }
}
