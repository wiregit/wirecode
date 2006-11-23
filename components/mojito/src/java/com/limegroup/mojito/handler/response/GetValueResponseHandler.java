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
import java.util.Collection;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.exceptions.DHTNoSuchElementException;
import com.limegroup.mojito.exceptions.DHTBackendException;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.Contact;

/**
 * The GetValueResponseHandler retrieves DHTValues from a remote Node
 */
public class GetValueResponseHandler extends AbstractResponseHandler<Collection<DHTValue>> {
        
    private Contact node;
    
    private KUID valueId;
    
    private Collection<KUID> nodeIds;
    
    public GetValueResponseHandler(Context context, 
            Contact node, KUID valueId, Collection<KUID> nodeIds) {
        super(context);
        
        this.node = node;
        this.valueId = valueId;
        this.nodeIds = nodeIds;
    }
    
    @Override
    protected void start() throws DHTException {
        super.start();
        
        FindValueRequest request = context.getMessageHelper()
            .createFindValueRequest(node.getContactAddress(), valueId, nodeIds);
        
        try {
            context.getMessageDispatcher().send(node, request, this);
        } catch (IOException err) {
            throw new DHTException(err);
        }
    }

    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        if (message instanceof FindValueResponse) {
            Collection<DHTValue> values = ((FindValueResponse)message).getValues();
            setReturnValue(values);
            
        // Imagine the following case: We do a lookup for a value 
        // on the 59th minute and start retrieving the values on 
        // the 60th minute. As values expire on the 60th minute 
        // it may no longer exists and the remote Node returns us
        // a Set of the k-closest Nodes instead.
        } else {
            setException(new DHTNoSuchElementException(message, 
                    node + " did not have " + valueId + "=" + nodeIds));
        }
    }
    
    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        fireTimeoutException(nodeId, dst, message, time);
    }
    
    @Override
    protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
        setException(new DHTBackendException(nodeId, dst, message, e));
    }
}