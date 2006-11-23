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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueBag;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.RequestMessage;

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
        
        Database database = context.getDatabase();
        DHTValueBag bag = database.get(lookup);

        if (bag == null || bag.isEmpty()) {
            // OK, send Contacts instead!
            findNodeDelegate.handleRequest(message);
            return;
        }
        
        Map<KUID, DHTValue> map = bag.getValuesMap();
        
        //TODO: should never be empty?
        if (!map.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Hit! " + lookup + "\n" +bag.toString());
            }
            
            Set<KUID> keys = Collections.emptySet();
            Collection<DHTValue> values = Collections.emptyList();
            
            Collection<KUID> nodeIds = request.getKeys();
            
            // Nothing specific requested?
            if (nodeIds.isEmpty()) {
                // If there's only one value for this key then send 
                // just the DHTValue
                if (map.size() == 1) {
                    values = map.values();
                    
                // Otherwise send the keys and the remote Node must
                // figure out what it's looking for
                } else {
                    keys = map.keySet();
                }
            } else {
                // Send all requested values back.
                // TODO: http://en.wikipedia.org/wiki/Knapsack_problem
                values = new ArrayList<DHTValue>(nodeIds.size());
                for (KUID nodeId : nodeIds) {
                    DHTValue value = map.get(nodeId);
                    if (value != null) {
                        values.add(value);
                    }
                }
            }
            
            context.getNetworkStats().FIND_VALUE_REQUESTS.incrementStat();
           
            FindValueResponse response = context.getMessageHelper()
                        .createFindValueResponse(request, keys, values, bag.incrementRequestLoad());
            context.getMessageDispatcher().send(request.getContact(), response);
        } else {
            // OK, send Contacts instead!
            findNodeDelegate.handleRequest(message);
        }
    }
}
