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
 
package de.kapsi.net.kademlia.handler.request;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.request.FindNodeRequest;
import de.kapsi.net.kademlia.messages.request.LookupRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.util.CollectionUtils;

/**
 * Handles FIND_NODE as well as FIND_VALUE requests
 */
public class LookupRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(LookupRequestHandler.class);
    
    public LookupRequestHandler(Context context) {
        super(context);
    }

    public void handleRequest(KUID nodeId, SocketAddress src, RequestMessage message) throws IOException {
        
        LookupRequest request = (LookupRequest)message;
        KUID lookup = request.getLookupID();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactNode.toString(nodeId, src) + " is trying to lookup " + lookup);
        }
        
        if (request instanceof FindNodeRequest) {
            handleFindNodeRequest(nodeId, src, request);
        } else {
            handleFindValueRequest(nodeId, src, request);
        }
    }
    
    private void handleFindNodeRequest(KUID nodeId, 
            SocketAddress src, LookupRequest request) throws IOException {
        
        KUID lookup = request.getLookupID();
        
        QueryKey queryKey = QueryKey.getQueryKey(src);
        List bucketList = Collections.EMPTY_LIST;
        
        if (context.isBootstrapped()) {
            bucketList = context.getRouteTable().select(lookup, 
                    KademliaSettings.REPLICATION_PARAMETER.getValue(), false, false);
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending back: " + CollectionUtils.toString(bucketList));
        }
        
        FindNodeResponse response = context.getMessageFactory()
                    .createFindNodeResponse(request, queryKey, bucketList);
        
        context.getMessageDispatcher().send(src, response, null);
    }
    
    private void handleFindValueRequest(KUID nodeId, 
            SocketAddress src, LookupRequest request) throws IOException {
        
        KUID lookup = request.getLookupID();
        
        Collection values = context.getDatabase().get(lookup);
        if (values != null && !values.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Hit! " + lookup + " = {" + CollectionUtils.toString(values) + "}");
            }
            
            FindValueResponse response = context.getMessageFactory()
                        .createFindValueResponse(request, values);
            context.getMessageDispatcher().send(src, response, null);
        } else {
            // OK, send ContactNodes instead!
            handleFindNodeRequest(nodeId, src, request);
        }
    }
}
