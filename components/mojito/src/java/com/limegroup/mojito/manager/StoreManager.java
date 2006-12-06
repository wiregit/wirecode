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
import java.util.concurrent.Callable;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.concurrent.AbstractDHTFuture;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.handler.response.StoreResponseHandler;
import com.limegroup.mojito.result.StoreResult;
import com.limegroup.mojito.routing.Contact;

/**
 * The StoreManager class manages 
 */
public class StoreManager extends AbstractManager<StoreResult> {
    
    public StoreManager(Context context) {
        super(context);
    }
    
    /**
     * Stores a single DHTValue on the DHT
     */
    public DHTFuture<StoreResult> store(DHTValue value) {
        return store(Collections.singleton(value));
    }
    
    /**
     * Stores a collection of DHTValues on the DHT. All DHTValues
     * must have the same valueId!
     */
    public DHTFuture<StoreResult> store(Collection<? extends DHTValue> values) {
        StoreResponseHandler handler = new StoreResponseHandler(context, values);
        StoreFuture future = new StoreFuture(handler);
        
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * Stores a collection of DHTValues at the given Contact.
     */
    public DHTFuture<StoreResult> store(Contact node, QueryKey queryKey, 
            Collection<? extends DHTValue> values) {
        
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
