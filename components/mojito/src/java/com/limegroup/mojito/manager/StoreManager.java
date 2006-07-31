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

import java.util.concurrent.Callable;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.event.StoreEvent;
import com.limegroup.mojito.handler.response.StoreResponseHandler;

/**
 * 
 */
public class StoreManager extends AbstractManager<StoreEvent> {
    
    public StoreManager(Context context) {
        super(context);
    }
    
    public DHTFuture<StoreEvent> store(DHTValue value) {
        StoreResponseHandler handler = new StoreResponseHandler(context, value);
        StoreFuture future = new StoreFuture(handler);
        
        context.execute(future);
        return future;
    }
    
    public DHTFuture<StoreEvent> store(Contact node, QueryKey queryKey, DHTValue value) {
        StoreResponseHandler handler = new StoreResponseHandler(context, node, queryKey, value);
        StoreFuture future = new StoreFuture(handler);
        context.execute(future);
        return future;
    }
    
    private class StoreFuture extends AbstractDHTFuture {
        
        public StoreFuture(Callable<StoreEvent> callable) {
            super(callable);
        }

        @Override
        protected void deregister() {
        }
    }
}
