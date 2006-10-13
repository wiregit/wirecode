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
    
    private DHTValueDatabase database = null;
    
    private int maxDatabaseSize = -1;
    
    private int maxValuesPerKey = -1;
    
    public DatabaseImpl() {
        this(-1, -1);
    }
    
    public DatabaseImpl(int maxDatabaseSize, int maxValuesPerKey) {
        this.maxDatabaseSize = maxDatabaseSize;
        this.maxValuesPerKey = maxValuesPerKey;
        
        database = new DHTValueDatabase();
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
            bag = new DHTValueBag(valueId);
        }
        
        if (bag.add(value)) {
            if (!database.containsKey(valueId)) {
                database.put(valueId, bag);
            }
            return true;
        }
        return false;
    }
    
    protected boolean canAddValue(Map<KUID, ? extends Map<KUID, DHTValue>> database, DHTValue value) {
        
        if (value.isLocalValue()) {
            return true;
        }
        
        KUID valueId = value.getValueID();
        KUID nodeId = value.getOriginatorID();
        Map<KUID, DHTValue> map = database.get(valueId);
        
        if (map == null) {
            return maxDatabaseSize < 0 || database.size() < maxDatabaseSize;
        } else if (maxValuesPerKey < 0 || map.size() < maxValuesPerKey) {
            return true;
        }
        
        return value.isDirect() && map.containsKey(nodeId);
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
    
    private synchronized boolean remove(DHTValue value, boolean remote) {
        if (!canRemoveValue(database, value)) {
            return false;
        }
        
        KUID valueId = value.getValueID();
        DHTValueBag bag = database.get(valueId);
        if (bag != null) {
            int size = bag.size();
            
            if (remote) {
                bag.remoteRemove(value);
            } else {
                bag.remove(value);
            }
            
            if (bag.isEmpty()) {
                database.remove(valueId);
            }
            
            if (bag.size() != size) {
                return true;
            }
        }
        return false;
    }
    
    protected boolean canRemoveValue(Map<KUID, ? extends Map<KUID, DHTValue>> database, DHTValue value) {
        return true;
    }
    
    public synchronized Map<KUID, DHTValue> get(KUID valueId) {
        DHTValueBag bag = database.get(valueId);
        if (bag != null) {
            return Collections.unmodifiableMap(new HashMap<KUID, DHTValue>(bag));
        }
        return Collections.emptyMap();
    }
    
    public synchronized boolean contains(DHTValue value) {
        return get(value.getValueID()).containsValue(value);
    }

    public synchronized Set<KUID> keySet() {
        return Collections.unmodifiableSet(new HashSet<KUID>(database.keySet()));
    }

    public synchronized Collection<DHTValue> values() {
        List<DHTValue> values = new ArrayList<DHTValue>();
        for (DHTValueBag bag : database.values()) {
            values.addAll(bag.values());
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
    private class DHTValueDatabase extends HashMap<KUID, DHTValueBag> {

        private static final long serialVersionUID = 2646628989065603637L;
        
    }
    
    /**
     * 
     */
    private class DHTValueBag extends HashMap<KUID, DHTValue> {
        
        private static final long serialVersionUID = 3677203930910637434L;

        private KUID valueId;
        
        private transient int modCount = 0;
        
        public DHTValueBag(KUID valueId) {
            this.valueId = valueId;
        }
        
        public boolean add(DHTValue value) {
            int modCount = this.modCount;
            put(value.getOriginatorID(), value);
            return modCount != this.modCount;
        }

        /**
         * Removes the given DHTValue from the bag if certain
         * conditions met like it must be a direct remove
         * request.
         */
        public DHTValue remoteRemove(DHTValue value) {
            if (value.isDirect() 
                    || value.isLocalValue()) {
                
                KUID originatorId = value.getOriginatorID();
                DHTValue current = get(originatorId);
                if (current != null) {
                    if (!current.isLocalValue()
                            || (current.isLocalValue() && value.isLocalValue())) {
                        return remove(originatorId);
                    }
                }
            }
            
            return null;
        }
        
        @Override
        public DHTValue put(KUID key, DHTValue value) {
            if (!valueId.equals(value.getValueID())) {
                throw new IllegalArgumentException("Value ID must be " + valueId);
            }
            
            if (!key.equals(value.getOriginatorID())) {
                throw new IllegalArgumentException("Originator ID must be " + key);
            }
            
            if ((value.isDirect() && putDirectDHTValue(key, value)) 
                    || (!value.isDirect() && putIndirectDHTValue(key, value))) {
                
                modCount++;
                return super.put(key, value);
            }
            
            return null;
        }
        
        /**
         * Returns whether or not the given (direct) DHTValue can
         * be added to the bag
         */
        private boolean putDirectDHTValue(KUID key, DHTValue value) {
            assert (value.isDirect());
            
            // Do not replace local with non-local values
            DHTValue current = get(key);
            if (current != null 
                    && current.isLocalValue() 
                    && !value.isLocalValue()) {
                return false;
            }
            
            return true;
        }
        
        /**
         * Returns whether or not the given (indirect) DHTValue can
         * be added to the bag
         */
        private boolean putIndirectDHTValue(KUID key, DHTValue value) {
            assert (!value.isDirect());
            assert (!value.isLocalValue()); // cannot be a local value
            
            // Do not overwrite an directly stored value
            DHTValue current = get(key);
            if (current != null 
                    && current.isDirect()) {
                return false;
            }
            
            return true;
        }
        
        @Override
        public DHTValue remove(Object key) {
            if (key instanceof DHTValue) {
                key = ((DHTValue)key).getOriginatorID();
            }
            
            int size = size();
            DHTValue value = super.remove(key);
            if (size != size()) {
                modCount++;
            }
            return value;
        }
    }
}
