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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.mojito.Context2;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.message2.MessageHelper2;
import org.limewire.mojito.message2.RequestMessage;
import org.limewire.mojito.message2.ValueRequest;
import org.limewire.mojito.message2.ValueResponse;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.DatabaseUtils;


/**
 * Handles incoming FIND_VALUE requests.
 */
public class ValueRequestHandler2 extends AbstractRequestHandler2 {
    
    private static final Log LOG 
        = LogFactory.getLog(ValueRequestHandler2.class);
    
    @InspectablePrimitive(value = "Value Not Found")
    private static final AtomicInteger NOT_FOUND_COUNT = new AtomicInteger();
    
    @InspectablePrimitive(value = "Value Found")
    private static final AtomicInteger FOUND_COUNT = new AtomicInteger();
    
    /**
     * Delegate to the FIND_NODE request handler
     */
    private final NodeRequestHandler2 node;
    
    public ValueRequestHandler2(Context2 context, NodeRequestHandler2 node) {
        super(context);
        
        this.node = node;
    }
    
    @Override
    protected void processRequest(RequestMessage message) throws IOException {
        
        ValueRequest request = (ValueRequest)message;
        
        KUID lookupId = request.getLookupId();
        DHTValueType valueType = request.getValueType();
        
        Map<KUID, DHTValueEntity> bag = null;
        float requestLoad = 0f;
        
        Database database = context.getDatabase();
        synchronized (database) {
            bag = database.get(lookupId);
            requestLoad = database.getRequestLoad(lookupId, true);
        }
        
        // The keys and values we'll return
        Set<KUID> availableKeys = new HashSet<KUID>();
        Set<DHTValueEntity> valuesToReturn = new HashSet<DHTValueEntity>();
        
        // The keys the remote Node is requesting
        KUID[] requestedSecondaryKeys = request.getSecondaryKeys();
        
        if (bag != null && !bag.isEmpty()) {
            availableKeys = new HashSet<KUID>();
            valuesToReturn = new HashSet<DHTValueEntity>();
            
            DHTValueEntity[] entities 
                = bag.values().toArray(new DHTValueEntity[0]);
            DHTValueEntity[] filtered 
                = DatabaseUtils.filter(valueType, entities);
            
            if (requestedSecondaryKeys.length == 0) {
                if (valuesToReturn.isEmpty()
                        && filtered.length == 1) {
                    valuesToReturn.addAll(Arrays.asList(filtered));
                } else {
                    for (DHTValueEntity entity : filtered) {
                        availableKeys.add(entity.getSecondaryKey());
                    }
                }
            } else {
                // Send all requested values back.
                // TODO: http://en.wikipedia.org/wiki/Knapsack_problem
                for (DHTValueEntity entity : filtered) {
                    KUID secondaryKey = entity.getSecondaryKey();
                    if (contains(requestedSecondaryKeys, secondaryKey)) {
                        valuesToReturn.add(entity);
                    }
                }
            }
        }
        
        if (valuesToReturn.isEmpty() 
                && availableKeys.isEmpty()) {
            
            NOT_FOUND_COUNT.incrementAndGet();
            
            if (LOG.isInfoEnabled()) {
                LOG.info("No values for " + lookupId 
                    + ", returning Contacts instead to " + request.getContact());
            }
            
            // OK, send Contacts instead!
            node.handleRequest(message);
            
        } else {
            
            FOUND_COUNT.incrementAndGet();
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Return " + CollectionUtils.toString(valuesToReturn) + " and " 
                        + CollectionUtils.toString(availableKeys) + " for " 
                        + lookupId + " to " + request.getContact());
            }
            
            MessageHelper2 messageHelper = context.getMessageHelper();
            ValueResponse response = messageHelper.createFindValueResponse(
                    request, requestLoad, 
                    valuesToReturn.toArray(new DHTValueEntity[0]), 
                    availableKeys.toArray(new KUID[0]));
            send(request.getContact(), response);
        }
    }
    
    private static boolean contains(Object[] c, Object element) {
        for (Object o : c) {
            if (o == element || (o != null && o.equals(element))) {
                return true;
            }
        }
        
        return false;
    }
}
