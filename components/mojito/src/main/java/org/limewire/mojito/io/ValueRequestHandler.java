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

package org.limewire.mojito.io;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.message.RequestMessage;
import org.limewire.mojito.message.ValueRequest;
import org.limewire.mojito.message.ValueResponse;
import org.limewire.mojito.storage.ValueTuple;
import org.limewire.mojito.storage.ValueType;
import org.limewire.mojito.storage.Database;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.DatabaseUtils;


/**
 * Handles incoming FIND_VALUE requests.
 */
public class ValueRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG 
        = LogFactory.getLog(ValueRequestHandler.class);
    
    @InspectablePrimitive(value = "Value Not Found")
    private static final AtomicInteger NOT_FOUND_COUNT = new AtomicInteger();
    
    @InspectablePrimitive(value = "Value Found")
    private static final AtomicInteger FOUND_COUNT = new AtomicInteger();
    
    /**
     * Delegate to the FIND_NODE request handler
     */
    private final NodeRequestHandler node;
    
    public ValueRequestHandler(Context context, NodeRequestHandler node) {
        super(context);
        
        this.node = node;
    }
    
    @Override
    protected void processRequest(RequestMessage message) throws IOException {
        
        ValueRequest request = (ValueRequest)message;
        
        KUID lookupId = request.getLookupId();
        ValueType valueType = request.getValueType();
        
        Map<KUID, ValueTuple> bag = null;
        float requestLoad = 0f;
        
        Database database = context.getDatabase();
        synchronized (database) {
            bag = database.get(lookupId);
            requestLoad = database.getRequestLoad(lookupId, true);
        }
        
        // The keys and values we'll return
        Set<KUID> availableKeys = new HashSet<KUID>();
        Set<ValueTuple> valuesToReturn = new HashSet<ValueTuple>();
        
        // The keys the remote Node is requesting
        KUID[] requestedSecondaryKeys = request.getSecondaryKeys();
        
        if (bag != null && !bag.isEmpty()) {
            availableKeys = new HashSet<KUID>();
            valuesToReturn = new HashSet<ValueTuple>();
            
            ValueTuple[] entities 
                = bag.values().toArray(new ValueTuple[0]);
            ValueTuple[] filtered 
                = DatabaseUtils.filter(valueType, entities);
            
            if (requestedSecondaryKeys.length == 0) {
                if (valuesToReturn.isEmpty()
                        && filtered.length == 1) {
                    valuesToReturn.addAll(Arrays.asList(filtered));
                } else {
                    for (ValueTuple entity : filtered) {
                        availableKeys.add(entity.getSecondaryKey());
                    }
                }
            } else {
                // Send all requested values back.
                // TODO: http://en.wikipedia.org/wiki/Knapsack_problem
                for (ValueTuple entity : filtered) {
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
            
            MessageHelper messageHelper = context.getMessageHelper();
            ValueResponse response = messageHelper.createFindValueResponse(
                    request, requestLoad, 
                    valuesToReturn.toArray(new ValueTuple[0]), 
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
