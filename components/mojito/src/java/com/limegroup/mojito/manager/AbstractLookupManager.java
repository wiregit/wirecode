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

import java.util.HashMap;
import java.util.Map;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.response.LookupResponseHandler;

/**
 * 
 */
abstract class AbstractLookupManager<V> extends AbstractManager<V> {
    
    private Map<KUID, LookupFuture> futureMap = new HashMap<KUID, LookupFuture>();
    
    public AbstractLookupManager(Context context) {
        super(context);
    }
    
    public void init() {
        synchronized(getLookupLock()) {
            futureMap.clear();
        }
    }
    
    private Object getLookupLock() {
        return futureMap;
    }
    
    public DHTFuture<V> lookup(KUID lookupId) {
        return lookup(lookupId, -1);
    }
    
    public DHTFuture<V> lookup(KUID lookupId, int count) {
        synchronized(getLookupLock()) {
            LookupFuture future = futureMap.get(lookupId);
            if (future == null) {
                LookupResponseHandler<V> handler = createLookupHandler(lookupId, count);
                
                future = new LookupFuture(lookupId, handler);
                futureMap.put(lookupId, future);
                
                context.execute(future);
            }
            
            return future;
        }
    }
    
    protected abstract LookupResponseHandler<V> createLookupHandler(KUID lookupId, int count);
    
    private class LookupFuture extends AbstractDHTFuture {

        private KUID lookupId;
        
        private LookupResponseHandler<V> handler;
        
        public LookupFuture(KUID lookupId, LookupResponseHandler<V> handler) {
            super(handler);
            this.lookupId = lookupId;
            this.handler = handler;
        }

        @Override
        protected void deregister() {
            synchronized(getLookupLock()) {
                futureMap.remove(lookupId);
            }
        }

        public String toString() {
            return "Future: " + lookupId + ", " + handler;
        }
    }
}
