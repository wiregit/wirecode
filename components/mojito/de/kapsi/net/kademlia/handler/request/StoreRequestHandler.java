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
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.StoreResponse;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.DatabaseSettings;
import de.kapsi.net.kademlia.settings.KademliaSettings;

public class StoreRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreRequestHandler.class);
    
    private final NetworkStatisticContainer networkStats;
    
    public StoreRequestHandler(Context context) {
        super(context);
        this.networkStats = context.getNetworkStats();
    }
    
    public void handleRequest(RequestMessage message) throws IOException {
        
        StoreRequest request = (StoreRequest)message;
        networkStats.STORE_REQUESTS.incrementStat();
        QueryKey queryKey = request.getQueryKey();
        
        if (queryKey == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(message.getContactNode() 
                        + " does not provide a QueryKey");
            }
            networkStats.STORE_REQUESTS_NO_QK.incrementStat();
            return;
        }
        
        QueryKey expected = QueryKey.getQueryKey(message.getSocketAddress());
        if (!expected.equals(queryKey)) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Expected " + expected + " from " 
                        + message.getContactNode() 
                        + " but got " + queryKey);
            }
            networkStats.STORE_REQUESTS_BAD_QK.incrementStat();
            return;
        }
        
        int remaining = request.getRemaingCount();
        Collection values = request.getValues();
        
        if (LOG.isTraceEnabled()) {
            if (!values.isEmpty()) {
                LOG.trace(message.getContactNode() 
                        + " requested us to store the KeyValues " + values);
            } else {
                LOG.trace(message.getContactNode()
                        + " requested us to store " + remaining + " KeyValues");
            }
        }
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        // Avoid to create an empty ArrayList
        List stats = (values.isEmpty() ? Collections.EMPTY_LIST : new ArrayList(values.size()));
        
        // Add the KeyValues...
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            KeyValue keyValue = (KeyValue)it.next();

            // under the assumption that the requester sent us a lookup before
            // check if we are part of the closest alive nodes to this value
            List nodesList = getRouteTable().select(keyValue.getKey(), k, false, false);
            if (!nodesList.contains(context.getLocalNode())) {
                nodesList = getRouteTable().select(keyValue.getKey(), k, true, false);
                if (!nodesList.contains(context.getLocalNode())) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("We are not close to " + keyValue.getKey() + ". KeyValue will expire faster!");
                    }
                    
                    context.getDataBaseStats().NOT_MEMBER_OF_CLOSEST_SET.incrementStat();
                    keyValue.setClose(false);
                }
            }
            
            try {
                if (context.getDatabase().add(keyValue)) {
                    networkStats.STORE_REQUESTS_OK.incrementStat();
                    stats.add(new StoreResponse.StoreStatus(keyValue.getKey(), StoreResponse.SUCCEEDED));
                } else {
                    networkStats.STORE_REQUESTS_FAILURE.incrementStat();
                    stats.add(new StoreResponse.StoreStatus(keyValue.getKey(), StoreResponse.FAILED));
                }
            } catch (SignatureException err) {
                networkStats.STORE_REQUESTS_FAILURE.incrementStat();
                stats.add(new StoreResponse.StoreStatus(keyValue.getKey(), StoreResponse.FAILED));
            } catch (InvalidKeyException err) {
                networkStats.STORE_REQUESTS_FAILURE.incrementStat();
                stats.add(new StoreResponse.StoreStatus(keyValue.getKey(), StoreResponse.FAILED));
            }
        }
        
        int maxOnce = DatabaseSettings.MAX_STORE_FORWARD_ONCE.getValue();
        int keyValues = Math.min(maxOnce, remaining);
        
        StoreResponse response 
            = context.getMessageFactory().createStoreResponse(request, keyValues, stats);
        context.getMessageDispatcher().send(request.getContactNode(), response, null);
    }
}
