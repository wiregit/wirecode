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

package com.limegroup.mojito.db.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueBag;
import com.limegroup.mojito.db.Database;

/*
 * Multiple values per key and one value per nodeId under a certain key
 * 
 * valueId
 *   nodeId
 *     value
 *   nodeId
 *     value
 * valueId
 *   nodeId
 *     value
 *   nodeId
 *     value
 *   nodeId
 *     value
 */

/**
 * This is a simple implementation of the Database interface.
 * 
 * TODO: For more advanced features we need some definition for
 * DHTValues (non-signed values cannot replace signed values and
 * what not).
 */
public class DatabaseImpl implements Database {
    
    private static final long serialVersionUID = -4857315774747734947L;
    
    private DHTValueDatabase<DHTValueBag> database = null;
    
    /**
     * The maximum database size. Can be negative to make the size unbound
     */
    private int maxDatabaseSize = -1;
    
    private int maxValuesPerKey = -1;
    
    public DatabaseImpl() {
        this(-1, -1);
    }
    
    public DatabaseImpl(int maxDatabaseSize, int maxValuesPerKey) {
        this.maxDatabaseSize = maxDatabaseSize;
        this.maxValuesPerKey = maxValuesPerKey;
        
        database = new DHTValueDatabase<DHTValueBag>();
    }
    
    public synchronized int getKeyCount() {
        return database.size();
    }
    
    public synchronized int getValueCount() {
        int count = 0;
        for (DHTValueBag bag : database.values()) {
            count += bag.size();
        }
        return count;
    }
    
    public synchronized void clear() {
        database.clear();
    }
    
    public synchronized boolean store(DHTValue value) {
        if (value.isEmpty()) {
            return remove(value, true);
        } else {
            return add(value);
        }
    }
    
    public synchronized boolean add(DHTValue value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException();
        }
        
        if (!canAddValue(database, value)) {
            return false;
        }
        
        KUID valueId = value.getValueID();
        DHTValueBag bag = database.get(valueId);
        
        if (bag == null) {
            bag = new DHTValueBagImpl(valueId);
        }
        
        if (bag.add(value)) {
            if (!database.containsKey(valueId)) {
                database.put(valueId, bag);
            }
            return true;
        }
        return false;
    }
    
    protected boolean canAddValue(Map<KUID, ? extends DHTValueBag> database, DHTValue value) {
        
        if (value.isLocalValue()) {
            return true;
        }
        
        KUID valueId = value.getValueID();
        KUID nodeId = value.getOriginatorID();
        DHTValueBag bag = database.get(valueId);
        
        if (bag == null) {
            return maxDatabaseSize < 0 || database.size() < maxDatabaseSize;
        } else if (maxValuesPerKey < 0 || bag.size() < maxValuesPerKey) {
            return true;
        }
        
        return value.isDirect() && bag.containsKey(nodeId);
    }
    
    public synchronized boolean remove(DHTValue value) {
        return remove(value, false);
    }
    
    public synchronized boolean removeAll(Collection<? extends DHTValue> values) {
        boolean removedAll = true;
        for (DHTValue value : values) {
            if (!remove(value)) {
                removedAll = false;
            }
        }
        return removedAll;
    }
    
    /**
     * Remove a value from the database. 

     * @param value The DHTValue to remove
     * @param isRemoteRemove Whether or not the value is 
     * removed by executing a store with empty data
     * 
     */
    private synchronized boolean remove(DHTValue value, boolean isRemoteRemove) {
        if (!canRemoveValue(database, value)) {
            return false;
        }
        
        KUID valueId = value.getValueID();
        DHTValueBag bag = database.get(valueId);
        if (bag != null) {
            int size = bag.size();
            
            bag.remove(value, isRemoteRemove);
            
            if (bag.isEmpty()) {
                database.remove(valueId);
            }
            
            if (bag.size() != size) {
                return true;
            }
        }
        return false;
    }
    
    protected boolean canRemoveValue(Map<KUID, ? extends DHTValueBag> database, DHTValue value) {
        return true;
    }
    
    public synchronized DHTValueBag get(KUID valueId) {
        return database.get(valueId);
    }
    
    public synchronized boolean contains(DHTValue value) {
        DHTValueBag bag = get(value.getValueID()); 
        
        if(bag != null) {
            return bag.containsKey(value.getOriginatorID());
        } 
        
        return false;
    }

    public synchronized Set<KUID> keySet() {
        return Collections.unmodifiableSet(new HashSet<KUID>(database.keySet()));
    }

    public synchronized Collection<DHTValue> values() {
        List<DHTValue> values = new ArrayList<DHTValue>();
        for (DHTValueBag bag : database.values()) {
            values.addAll(bag.getAllValues());
        }
        return Collections.unmodifiableList(values);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
    
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        for(DHTValue value : values()) {    
            buffer.append(value).append("\n\n");
        }
        buffer.append("-------------\n");
        buffer.append("TOTAL: ").append(getKeyCount())
            .append("/").append(getValueCount()).append("\n");
        return buffer.toString();
    }
    
    /**
     * 
     */
    private class DHTValueDatabase<V extends DHTValueBag> extends HashMap<KUID, V> {

        private static final long serialVersionUID = 2646628989065603637L;
        
    }
}
