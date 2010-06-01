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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.mojito.KUID;
import org.limewire.mojito.settings.DatabaseSettings;
/**
 * Stores a {@link ValueTuple} in a map.
 */
public class ValueTupleBag implements Serializable {
    
    private static final long serialVersionUID = -439172537233232473L;

    private static final float SMOOTHING_FACTOR 
        = DatabaseSettings.VALUE_REQUEST_LOAD_SMOOTHING_FACTOR.getValue();

    private static final int LOAD_NULLING_DELAY 
        = DatabaseSettings.VALUE_REQUEST_LOAD_NULLING_DELAY.getValue();

    private final Database database;
    
    private final KUID primaryKey;
    
    private final Map<KUID, ValueTuple> values 
        = new HashMap<KUID, ValueTuple>();

    /**
     * The request load associated with this DHT value bag
     */
    private transient float requestLoad = 0f;
    
    /**
     * The time the request load was last updated
     */
    private transient long lastRequestTime;
    
    ValueTupleBag(Database database, KUID primaryKey) {
        this.database = database;
        this.primaryKey = primaryKey;
    }
    
    public Database getDatabase() {
        return database;
    }
    
    public KUID getPrimaryKey() {
        return primaryKey;
    }
    
    public float getRequestLoad(boolean incrementLoad) {
        if (incrementLoad) {
            incrementRequestLoad();
        }
        return requestLoad;
    }
    
    public float incrementRequestLoad() {
        
        //Use Exponentially weighted moving average (EMA) to compute load
        //on this particular value bag. 
        //See http://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average
        
        long now = System.currentTimeMillis();
        
        if(lastRequestTime == 0L) {
            lastRequestTime = now;
            requestLoad = 0f;
            return requestLoad;
        }
        
        //we don't want to skew the results with delays that are too small!
        float delay = Math.max((now - lastRequestTime)/1000f, 0.01f); //in sec
        
        if (delay <= 0f || delay > LOAD_NULLING_DELAY) {
            lastRequestTime = now;
            requestLoad = 0f; //we can't trust the value anymore
            return requestLoad;
        }
        
        //The new requests per second is always a percentage of the 
        //increase over the old one:
        //newValue = SF*newLoad + (1 - SF)*previousValue
        //Which is equal to:
        //newValue = previousValue + SF * (newLoad - previousValue)
        requestLoad = requestLoad 
            + SMOOTHING_FACTOR*((1f/delay) - requestLoad);
        lastRequestTime = now;
        
        return requestLoad;
    }
    
    public boolean add(ValueTuple entity) {
        if (entity == null) {
            throw new NullPointerException("DHTValueEntity is null");
        }
        
        if (!primaryKey.equals(entity.getPrimaryKey())) {
            throw new IllegalArgumentException();
        }
        
        values.put(entity.getSecondaryKey(), entity);
        return true;
    }
    
    public ValueTuple get(KUID secondaryKey) {
        return values.get(secondaryKey);
    }
    
    public ValueTuple remove(KUID secondaryKey) {
        return values.remove(secondaryKey);
    }
    
    public boolean contains(KUID secondaryKey) {
        return values.containsKey(secondaryKey);
    }
    
    public int size() {
        return values.size();
    }
    
    public boolean isEmpty() {
        return values.isEmpty();
    }
    
    @Override
    public int hashCode() {
        return primaryKey.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ValueTupleBag)) {
            return false;
        }
        
        return primaryKey.equals(((ValueTupleBag)o).primaryKey);
    }
    
    public Map<KUID, ValueTuple> getValues(boolean copy) {
        if (copy) {
            return Collections.unmodifiableMap(
                    new HashMap<KUID, ValueTuple>(values));
        } else {
            return Collections.unmodifiableMap(values);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Bag: ").append(getPrimaryKey()).append("\n");
        buffer.append("Load: ").append(getRequestLoad(false)).append("\n");
        buffer.append("Values:").append("\n");
        
        for(ValueTuple entity : values.values()) {    
            buffer.append(entity).append("\n");  
        }
        
        return buffer.toString();
    }
}
