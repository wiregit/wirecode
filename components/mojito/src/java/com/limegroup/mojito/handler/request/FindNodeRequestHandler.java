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

package com.limegroup.mojito.handler.request;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.AbstractRequestHandler;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.util.BucketUtils;
import com.limegroup.mojito.util.CollectionUtils;

/**
 * The FindNodeRequestHandler handles incoming FIND_NODE requests
 */
public class FindNodeRequestHandler extends AbstractRequestHandler {

    private static final Log LOG = LogFactory.getLog(FindNodeRequestHandler.class);
    
    public FindNodeRequestHandler(Context context) {
        super(context);
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
    @Override
    protected void request(RequestMessage message) throws IOException {
        FindNodeRequest request = (FindNodeRequest)message;

        KUID lookup = request.getLookupID();
        
        Contact node = request.getContact();
        SocketAddress addr = node.getSourceAddress();
        QueryKey queryKey = QueryKey.getQueryKey(addr);
        
        List<Contact> nodes = Collections.emptyList();
        if (!context.isBootstrapping()) {
            if(context.isFirewalled()) {
                nodes = BucketUtils.getMostRecentlySeenContacts(
                            context.getRouteTable().getContacts(), 
                            KademliaSettings.REPLICATION_PARAMETER.getValue());
                
                // If the external port is not set then make sure
                // we're not in the list!
                if (context.getExternalPort() == 0) {
                    nodes.remove(context.getLocalNode());
                }
                
            } else {
                nodes = context.getRouteTable().select(lookup, 
                        KademliaSettings.REPLICATION_PARAMETER.getValue(), true);
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
        
        context.getMessageDispatcher().send(node, response);
    }
}
