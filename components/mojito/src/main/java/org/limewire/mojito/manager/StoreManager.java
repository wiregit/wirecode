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

package org.limewire.mojito.manager;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.limewire.mojito.Context;
import org.limewire.mojito.concurrent.AbstractDHTFuture;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.handler.response.StoreResponseHandler;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.QueryKey;


/**
 * The StoreManager class manages 
 */
public class StoreManager extends AbstractManager<StoreResult> {
    
    public StoreManager(Context context) {
        super(context);
    }
    
    /**
     * Stores a collection of DHTValues on the DHT. All DHTValues
     * must have the same valueId!
     */
    public DHTFuture<StoreResult> store(Collection<? extends DHTValueEntity> values) {
        StoreResponseHandler handler = new StoreResponseHandler(context, values);
        StoreFuture future = new StoreFuture(handler);
        
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * Stores a collection of DHTValues at the given Contact.
     */
    public DHTFuture<StoreResult> store(Contact node, QueryKey queryKey, 
            Collection<? extends DHTValueEntity> values) {
        
        StoreResponseHandler handler 
            = new StoreResponseHandler(context, node, queryKey, values);
        StoreFuture future = new StoreFuture(handler);
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * A store specific implementation of DHTFuture
     */
    private class StoreFuture extends AbstractDHTFuture<StoreResult> {
        
        public StoreFuture(Callable<StoreResult> callable) {
            super(callable);
        }
    }
}
