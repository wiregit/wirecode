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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.handler.AbstractRequestHandler;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;
import com.limegroup.mojito.messages.StoreResponse.StoreStatus;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;

/**
 * The StoreRequestHandler handles incoming store requests as
 * sent by other Nodes. It performs some probabilty tests to
 * make sure the request makes sense (i.e. if the Key is close
 * to us and so on).
 */
public class StoreRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreRequestHandler.class);
    
    private final NetworkStatisticContainer networkStats;
    
    public StoreRequestHandler(Context context) {
        super(context);
        this.networkStats = context.getNetworkStats();
    }
    
    @Override
    public void request(RequestMessage message) throws IOException {
        
        StoreRequest request = (StoreRequest)message;
        networkStats.STORE_REQUESTS.incrementStat();
        QueryKey queryKey = request.getQueryKey();
        
        if (queryKey == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(request.getContact() 
                        + " does not provide a QueryKey");
            }
            networkStats.STORE_REQUESTS_NO_QK.incrementStat();
            return;
        }
        
        QueryKey expected = QueryKey.getQueryKey(
                request.getContact().getContactAddress());
        
        if (!expected.equals(queryKey)) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Expected " + expected + " from " 
                        + request.getContact() 
                        + " but got " + queryKey);
            }
            networkStats.STORE_REQUESTS_BAD_QK.incrementStat();
            return;
        }
        
        DHTValue value = request.getDHTValue();
        KUID valueId = value.getValueID();
        
        // under the assumption that the requester sent us a lookup before
        // check if we are part of the closest alive nodes to this value
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        List<Contact> nodes = context.getRouteTable().select(valueId, k, false);
        
        if (!nodes.contains(context.getLocalNode())) {
            //try getting only nodes that have never failed, i.e. a larger key-space
            nodes = context.getRouteTable().select(valueId, k, true);
            if (!nodes.contains(context.getLocalNode())) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("We are not close to " + valueId 
                            + ". KeyValue will expire faster!");
                }
                
                context.getDatabaseStats().NOT_MEMBER_OF_CLOSEST_SET.incrementStat();
                value.setNearby(false);
            }
        }
        
        StoreStatus status = StoreStatus.FAILED;
        if (context.getDatabase().add(value)) {
            networkStats.STORE_REQUESTS_OK.incrementStat();
            status = StoreStatus.SUCCEEDED;
        } else {
            networkStats.STORE_REQUESTS_FAILURE.incrementStat();
        }
        
        StoreResponse response 
            = context.getMessageHelper().createStoreResponse(request, valueId, status);
        context.getMessageDispatcher().send(request.getContact(), response);
    }
}
