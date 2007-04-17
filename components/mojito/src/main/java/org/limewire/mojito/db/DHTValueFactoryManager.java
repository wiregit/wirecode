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

package org.limewire.mojito.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.mojito.db.impl.DefaultDHTValueFactory;

/**
 * The DHTValueFactoryManager manages DHTValueFactories
 */
public class DHTValueFactoryManager {
    
    public static final DHTValueFactory defaultFactory = new DefaultDHTValueFactory();

    private final Map<DHTValueType, DHTValueFactory> factories 
        = Collections.synchronizedMap(new HashMap<DHTValueType, DHTValueFactory>());
    
    /**
     * Adds a new DHTValueFactory
     */
    public DHTValueFactory addDHTValueFactory(DHTValueType valueType, DHTValueFactory factory) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        if (factory == null) {
            throw new NullPointerException("DHTValueFactory is null");
        }
        
        return factories.put(valueType, factory);
    }
    
    /**
     * Removes a DHTValueFactory that is registed under the given DHTValueType
     */
    public DHTValueFactory removeDHTValueFactory(DHTValueType valueType) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        return factories.remove(valueType);
    }
    
    /**
     * Returns a DHTValueFactory for the given DHTValueType or the defaultFactory
     * if none exists
     */
    public DHTValueFactory getDHTValueFactory(DHTValueType valueType) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        DHTValueFactory factory = factories.get(valueType);
        
        if (factory != null) {
            return factory;
        }
        
        return defaultFactory;
    }
}
