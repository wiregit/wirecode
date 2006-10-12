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

package com.limegroup.mojito.manager;

import java.util.Collection;
import java.util.Collections;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.concurrent.AbstractDHTFuture;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.handler.response.GetValueResponseHandler;

/**
 * GetValueManager is in widest sense equivalent to FindValueManager
 * but the main difference is that it's used to retrieve the value
 * rather than to find it. That means you must know which Node is storing
 * a value!
 */
public class GetValueManager extends AbstractManager<Collection<DHTValue>> {

    public GetValueManager(Context context) {
        super(context);
    }
    
    /**
     * Tries to get a value from the remote Node
     */
    public DHTFuture<Collection<DHTValue>> get(Contact node, 
            KUID valueId, KUID nodeId) {
        return get(node, valueId, Collections.singleton(nodeId));
    }
    
    /**
     * Tries to get one or more values from the remote Node
     */
    public DHTFuture<Collection<DHTValue>> get(Contact node, 
            KUID valueId, Collection<KUID> nodeIds) {
        
        GetValueResponseHandler handler 
            = new GetValueResponseHandler(context, node, valueId, nodeIds);
        GetValueFuture future = new GetValueFuture(handler);
        context.execute(future);
        return future;
    }
    
    /**
     * A "get value" specific implementation of DHTFuture
     */
    private class GetValueFuture extends AbstractDHTFuture<Collection<DHTValue>> {

        public GetValueFuture(GetValueResponseHandler callable) {
            super(callable);
        }
    }
}
