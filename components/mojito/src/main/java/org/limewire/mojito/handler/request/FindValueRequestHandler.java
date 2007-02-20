/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito.handler.request;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.util.CollectionUtils;


/**
 * The FindNodeRequestHandler handles incoming FIND_VALUE requests
 */
public class FindValueRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(FindValueRequestHandler.class);
    
    /**
     * Delegate to the FIND_NODE request handler
     */
    private FindNodeRequestHandler findNodeDelegate;
    
    public FindValueRequestHandler(Context context, 
            FindNodeRequestHandler findNodeDelegate) {
        super(context);
        
        this.findNodeDelegate = findNodeDelegate;
    }
    
    @Override
    protected void request(RequestMessage message) throws IOException {
        FindValueRequest request = (FindValueRequest)message;
        
        KUID lookup = request.getLookupID();
        
        Map<KUID, DHTValueEntity> bag = null;
        float requestLoad = 0f;
        
        Database database = context.getDatabase();
        synchronized (database) {
            bag = database.get(lookup);
            requestLoad = database.getRequestLoad(lookup, true);
        }

        // The keys and values we'll return
        Collection<KUID> availableKeys = new HashSet<KUID>();
        Collection<DHTValueEntity> valuesToReturn = new HashSet<DHTValueEntity>();
        
        // The keys the remote Node is requesting
        Collection<KUID> secondaryKeys = request.getSecondaryKeys();
        
        if (bag != null && !bag.isEmpty()) {
            if (secondaryKeys.isEmpty()) {
                if (valuesToReturn.isEmpty()
                        && bag.size() == 1) {
                    valuesToReturn.addAll(bag.values());
                } else {
                    availableKeys.addAll(bag.keySet());
                }
            } else {
                // Send all requested values back.
                // TODO: http://en.wikipedia.org/wiki/Knapsack_problem
                for (KUID secondaryKey : secondaryKeys) {
                    DHTValueEntity value = bag.get(secondaryKey);
                    if (value != null) {
                        valuesToReturn.add(value);
                    }
                }
            }   
        }
        
        if (valuesToReturn.isEmpty() 
                && availableKeys.isEmpty()) {
            
            if (LOG.isInfoEnabled()) {
                LOG.info("No values for " + lookup + ", returning Contacts instead to " + request.getContact());
            }
            
            // OK, send Contacts instead!
            findNodeDelegate.handleRequest(message);
            
        } else {
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Return " + CollectionUtils.toString(valuesToReturn) + " and " 
                        + CollectionUtils.toString(availableKeys) + " for " 
                        + lookup + " to " + request.getContact());
            }
            
            context.getNetworkStats().FIND_VALUE_REQUESTS.incrementStat();

            FindValueResponse response = context.getMessageHelper()
                .createFindValueResponse(request, availableKeys, 
                        valuesToReturn, requestLoad);
            context.getMessageDispatcher().send(request.getContact(), response);
        }
    }
}
