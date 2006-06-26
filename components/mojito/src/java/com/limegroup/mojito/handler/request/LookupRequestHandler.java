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
 
package com.limegroup.mojito.handler.request;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.handler.AbstractRequestHandler;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.LookupRequest;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.util.BucketUtils;
import com.limegroup.mojito.util.CollectionUtils;


/**
 * The LookupRequestHandler handles incoming FIND_NODE as well as 
 * FIND_VALUE requests.
 */
public class LookupRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(LookupRequestHandler.class);
    
    public LookupRequestHandler(Context context) {
        super(context);
    }

    public void handleRequest(RequestMessage message) throws IOException {
        
        LookupRequest request = (LookupRequest)message;
        KUID lookup = request.getLookupID();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(request.getContact() + " is trying to lookup " + lookup);
        }
        
        if (request instanceof FindNodeRequest) {
            handleFindNodeRequest(request);
        } else {
            handleFindValueRequest(request);
        }
    }
    
    /**
     * This method returns a list of nodes along with a QueryKey generated for this node. 
     * The QueryKey will can then be used by the querying node to store data at this node.
     * 
     * If the local node is passive (e.g. firewalled), it returns a list of Most Recently Seen 
     * nodes instead of returning the closest nodes to the lookup key. The reason for this is that 
     * passive nodes do not have accurate routing tables.
     * 
     * @param request The LookupRequest for this lookup
     * @throws IOException
     */
    private void handleFindNodeRequest(LookupRequest request) throws IOException {
        
        KUID lookup = request.getLookupID();
        QueryKey queryKey = QueryKey.getQueryKey(
                request.getContact().getSocketAddress());
        
        List<Contact> nodes = Collections.emptyList();
        if (!context.isBootstrapping()) {
            if(context.isFirewalled()) {
                nodes = context.getRouteTable().getContacts();
                nodes = BucketUtils.sort(nodes);
                nodes = nodes.subList(0, Math.min(nodes.size(), KademliaSettings.REPLICATION_PARAMETER.getValue()));
            } else {
                nodes = context.getRouteTable().select(lookup, 
                        KademliaSettings.REPLICATION_PARAMETER.getValue(), false, false);
            }
        }
        
        if (LOG.isTraceEnabled()) {
            if (!nodes.isEmpty()) {
                LOG.trace("Sending back: " + CollectionUtils.toString(nodes));
            } else {
                LOG.trace("Sending back an empty List");
            }
        }
        
        context.getNetworkStats().LOOKUP_REQUESTS.incrementStat();
        
        FindNodeResponse response = context.getMessageHelper()
                    .createFindNodeResponse(request, queryKey, nodes);
        
        context.getMessageDispatcher().send(request.getContact(), response);
    }
    
    private void handleFindValueRequest(LookupRequest request) throws IOException {
        
        KUID lookup = request.getLookupID();
        
        Collection<KeyValue> values = context.getDatabase().get(lookup);
        if (values != null && !values.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Hit! " + lookup + " = {" + CollectionUtils.toString(values) + "}");
            }
            
            FindValueResponse response = context.getMessageHelper()
                        .createFindValueResponse(request, values);
            context.getMessageDispatcher().send(request.getContact(), response);
        } else {
            // OK, send ContactNodes instead!
            handleFindNodeRequest(request);
        }
    }
}
