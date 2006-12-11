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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.concurrent.AbstractDHTFuture;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.result.Result;

/**
 * The AbstractLookupManager class manages lookups for Nodes or Values
 * and makes sure there are never two or more parallel lookups for
 * the same key.
 */
abstract class AbstractLookupManager<V extends Result> extends AbstractManager<V> {
    
    private final Map<KUID, LookupFuture> futureMap = 
        Collections.synchronizedMap(new HashMap<KUID, LookupFuture>());
    
    public AbstractLookupManager(Context context) {
        super(context);
    }
    
    public void init() {
        futureMap.clear();
    }
    
    /**
     * Starts a lookup for the given KUID
     */
    public DHTFuture<V> lookup(KUID lookupId) {
        return lookup(lookupId, -1);
    }
    
    /**
     * Starts a lookup for the given KUID and expects 'count' 
     * number of results
     */
    private DHTFuture<V> lookup(KUID lookupId, int count) {
        LookupFuture future;
        synchronized(futureMap) {
            future = futureMap.get(lookupId);
            if (future == null) {
                Callable<V> handler = createLookupHandler(lookupId, count);
                
                future = new LookupFuture(lookupId, handler);
                futureMap.put(lookupId, future);
                
            } else
                return future;
        }
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * A factory method to create LookupResponseHandler(s)
     * 
     * @param lookupId The KUID we're looking for
     * @param count The result set size (i.e. k)
     */
    protected abstract Callable<V> createLookupHandler(KUID lookupId, int count);
    
    /**
     * A lookup specific implementation of DHTFuture
     */
    private class LookupFuture extends AbstractDHTFuture<V> {

        private final KUID lookupId;
        
        private final Callable<V> handler;
        
        public LookupFuture(KUID lookupId, Callable<V> handler) {
            super(handler);
            this.lookupId = lookupId;
            this.handler = handler;
        }

        @Override
        protected void deregister() {
            futureMap.remove(lookupId);
        }

        public String toString() {
            return "Future: " + lookupId + ", " + handler;
        }
    }
}
