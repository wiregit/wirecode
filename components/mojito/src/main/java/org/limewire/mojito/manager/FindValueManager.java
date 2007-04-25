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

package org.limewire.mojito.manager;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.handler.response.FindValueResponseHandler;
import org.limewire.mojito.result.FindValueResult;


/**
 * FindValueManager manages lookups for values
 */
public class FindValueManager extends AbstractManager<FindValueResult> {
    
    private final Map<LookupKey, FindValueFuture> futureMap = 
        Collections.synchronizedMap(new HashMap<LookupKey, FindValueFuture>());
    
    public FindValueManager(Context context) {
        super(context);
    }

    public void init() {
        futureMap.clear();
    }
    
    /**
     * Starts a lookup for the given KUID
     */
    public DHTFuture<FindValueResult> lookup(KUID lookupId, DHTValueType valueType) {
        return lookup(lookupId, valueType, -1);
    }
    
    /**
     * Starts a lookup for the given KUID and expects 'count' 
     * number of results
     */
    private DHTFuture<FindValueResult> lookup(KUID lookupId, DHTValueType valueType, int count) {
        LookupKey lookupKey = new LookupKey(lookupId, valueType);
        
        FindValueFuture future = null;
        synchronized(futureMap) {
            future = futureMap.get(lookupKey);
            if (future == null) {
                FindValueResponseHandler handler 
                    = new FindValueResponseHandler(context, lookupId, valueType);
                
                future = new FindValueFuture(lookupKey, handler);
                futureMap.put(lookupKey, future);
                context.getDHTExecutorService().execute(future);
            }
        }
        
        return future;
    }
    
    /**
     * The DHTFuture for FIND_VALUE
     */
    private class FindValueFuture extends DHTFutureTask<FindValueResult> {

        private final LookupKey lookupKey;
        
        private final DHTTask<FindValueResult> handler;
        
        public FindValueFuture(LookupKey lookupKey, DHTTask<FindValueResult> handler) {
            super(context, handler);
            this.lookupKey = lookupKey;
            this.handler = handler;
        }

        @Override
        protected void done() {
            futureMap.remove(lookupKey);
        }

        public String toString() {
            return "FindValueFuture: " + lookupKey + ", " + handler;
        }
    }
    
    private static class LookupKey implements Serializable {
        
        private static final long serialVersionUID = -5207671662820873450L;

        private final KUID lookupId;
        
        private final DHTValueType valueType;
        
        private final int hashCode;
        
        private LookupKey(KUID lookupId, DHTValueType valueType) {
            this.lookupId = lookupId;
            this.valueType = valueType;
            this.hashCode = 17*lookupId.hashCode() + valueType.hashCode();
        }
        
        public int hashCode() {
            return hashCode;
        }
        
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof LookupKey)) {
                return false;
            }
            
            LookupKey other = (LookupKey)o;
            return lookupId.equals(other.lookupId)
                    && valueType.equals(other.valueType);
        }
        
        public String toString() {
            return "LookupKey: " + lookupId + ", " + valueType;
        }
    }
}
