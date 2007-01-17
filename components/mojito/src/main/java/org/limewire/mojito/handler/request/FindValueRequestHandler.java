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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueBag;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.RequestMessage;


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

        boolean empty = false;
        
        // The keys and values we'll return
        Collection<KUID> availableKeys = Collections.emptySet();
        Collection<DHTValueEntity> valuesToResturn = Collections.emptyList();
        
        if (bag != null) {
            synchronized(bag.getValuesLock()) {
                Map<KUID, DHTValueEntity> map = bag.getValuesMap();
                
                if (map.isEmpty()) {
                    empty = true;
                    
                } else {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Hit! " + lookup + "\n" + bag);
                    }
    
                    // The keys the remote Node is requesting
                    Collection<KUID> secondaryKeys = request.getSecondaryKeys();
    
                    // Nothing specific requested?
                    if (secondaryKeys.isEmpty()) {
                        // If there's only one value for this key then send 
                        // just the value
                        if (map.size() == 1) {
                            valuesToResturn = new ArrayList<DHTValueEntity>(map.values());
    
                            // Otherwise send the keys and the remote Node must
                            // figure out what it's looking for
                        } else {
                            availableKeys = new HashSet<KUID>(map.keySet());
                        }
                    } else {
                        // Send all requested values back.
                        // TODO: http://en.wikipedia.org/wiki/Knapsack_problem
                        valuesToResturn = new ArrayList<DHTValueEntity>(secondaryKeys.size());
                        for (KUID secondaryKey : secondaryKeys) {
                            DHTValueEntity value = map.get(secondaryKey);
                            if (value != null) {
                                valuesToResturn.add(value);
                            }
                        }
                    }
                }
            }
        } else {
            empty = true;
        }

        if (empty) {
            // OK, send Contacts instead!
            findNodeDelegate.handleRequest(message);
        } else {
            context.getNetworkStats().FIND_VALUE_REQUESTS.incrementStat();

            FindValueResponse response = context.getMessageHelper()
            .createFindValueResponse(request, availableKeys, valuesToResturn, 
                    bag.incrementRequestLoad());
            context.getMessageDispatcher().send(request.getContact(), response);
        }
    }
}
