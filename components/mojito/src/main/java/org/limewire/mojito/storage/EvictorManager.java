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

package org.limewire.mojito.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.mojito.routing.RouteTable;

/**
 * Manages instances of an {@link Evictor}.
 */
public class EvictorManager {
    
    public static final Evictor defaultEvictor = new DefaultEvictor();
    
    private final Map<ValueType, Evictor> evictors 
        = Collections.synchronizedMap(new HashMap<ValueType, Evictor>());
    
    /**
     * Registers a {@link Evictor} under the given {@link ValueType}.
     */
    public Evictor addEvictor(ValueType valueType, Evictor evictor) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        if (evictor == null) {
            throw new NullPointerException("Evictor is null");
        }
        
        return evictors.put(valueType, evictor);
    }
    
    /**
     * Removes and returns a {@link Evictor} that is registered under the
     * given {@link ValueType}.
     */
    public Evictor removeEvictor(ValueType valueType) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        return evictors.remove(valueType);
    }
    
    /**
     * Returns the {@link Evictor} for the given {@link ValueType}
     * or the {@link DefaultEvictor} if none exists.
     */
    public Evictor getEvictor(ValueType valueType) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        Evictor evictor = evictors.get(valueType);
        if (evictor != null) {
            return evictor;
        }
        
        return defaultEvictor;
    }
    
    /**
     * A helper method that delegates to a dedicated {@link Evictor}.
     */
    public boolean isExpired(RouteTable routeTable, ValueTuple entity) {
        ValueType valueType = entity.getValue().getValueType();
        return getEvictor(valueType).isExpired(routeTable, entity);
    }
}
