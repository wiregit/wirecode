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
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.util.CollectionUtils;

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
public class DatabaseImpl implements Database {

    private static final long serialVersionUID = -4857315774747734947L;
    
    private DHTValueDatabase database;
    
    private transient volatile DHTValueCollection values = null;
    
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
            return bag;
        }
        return Collections.emptyMap();
    }
    
    public synchronized boolean contains(DHTValue value) {
        return get(value.getValueID()).containsKey(value.getOriginatorID());
    }
    
    public synchronized Set<KUID> keySet() {
        return database.keySet();
    }
    
    public synchronized Collection<DHTValue> values() {
        if (values == null) {
            values = new DHTValueCollection();
        }
        return values;
    }
    
    public synchronized String toString() {
        return CollectionUtils.toString(values());
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
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
        
        private transient Object removeLock;
        private transient boolean removeEmptyBag;
        
        public DHTValueBag(KUID valueId) {
            this.valueId = valueId;
            initValueBag();
        }
        
        private void initValueBag() {
            removeLock = new Object();
            removeEmptyBag = true;
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
                
                if (isEmpty() && !database.containsKey(valueId)) {
                    // Register this Bag under the valueId if
                    // it isn't already.
                    database.put(valueId, this);
                }
                
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
                
                synchronized (removeLock) {
                    if (removeEmptyBag && isEmpty()) {
                        database.remove(valueId);
                    }
                }
            }
            return value;
        }

        @Override
        public Set<KUID> keySet() {
            return new Delegate<KUID>(this, super.keySet());
        }
        
        @Override
        public Set<Map.Entry<KUID, DHTValue>> entrySet() {
            return new Delegate<Map.Entry<KUID, DHTValue>>(this, super.entrySet());
        }
        
        @Override
        public Collection<DHTValue> values() {
            return new Delegate<DHTValue>(this, super.values());
        }
        
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
        }
        
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            initValueBag();
        }
    }
    
    /**
     * A collection of DHTValues. It acts a view of the underlying Map.
     */
    private class DHTValueCollection extends AbstractCollection<DHTValue> {

        @Override
        public boolean add(DHTValue o) {
            return DatabaseImpl.this.store(o);
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof DHTValue)) {
                return false;
            }
            
            return DatabaseImpl.this.contains((DHTValue)o);
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof DHTValue)) {
                return false;
            }
            
            return DatabaseImpl.this.remove((DHTValue)o);
        }

        @Override
        public Iterator<DHTValue> iterator() {
            return new DHTValueIterator();
        }

        @Override
        public int size() {
            return getValueCount();
        }
    }
    
    /**
     * An iterator that iterates through all DHTValueBags
     * and returns the DHTValues
     */
    private class DHTValueIterator implements Iterator<DHTValue> {
        
        private Iterator<DHTValueBag> bags = database.values().iterator();
        
        private Iterator<DHTValue> values = null;
        
        private DHTValueBag currentBag = null;
        
        private DHTValue currentValue = null;
        
        public boolean hasNext() {
            if (values != null && values.hasNext()) {
                return true;
            }
            
            if (bags.hasNext()) {
                currentBag = bags.next();
                values = currentBag.values().iterator();
                return hasNext();
            }
            
            return false;
        }

        public DHTValue next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            currentValue = values.next();
            return currentValue;
        }

        public void remove() {
            if (currentBag == null || currentValue == null) {
                throw new IllegalStateException();
            }
            
            synchronized (currentBag.removeLock) {
                try {
                    // Disable the auto removal of bags from the database 
                    // Map when the bag becomes empty as we're holding an 
                    // iterator of the database Map which would throw an
                    // ConcurrentModificationException as soon as we're 
                    // modifying the Map directly. Use Iterator.remove() 
                    // instead to remove empty DHTValueBags manually.
                    currentBag.removeEmptyBag = false;
                    
                    values.remove();
                    currentValue = null;
                    
                    if (currentBag.isEmpty()) {
                        bags.remove();
                    }
                } finally {
                    // and enable it again
                    currentBag.removeEmptyBag = true;
                }
            }
            
            if (currentBag.isEmpty()) {
                currentBag = null;
            }
        }
    }
    
    /**
     * This little delegation hack is necessary because the remove() 
     * operators of the various views of Map (namely entrySet(), 
     * keySet() and values()) do not call Map.remove() but rather do 
     * call an internal method which we cannot overwrite.
     * 
     * That means if we're using a view to remove all items we'll end 
     * up with empty DHTValueBag entries in the 'database' since
     * DHTValueBag's remove() gets never called which would take care
     * of cleaining everything up.
     */
    private class Delegate<E> implements Set<E>, Collection<E> {
        
        private DHTValueBag bag;
        private Collection<E> view;
        
        @SuppressWarnings("unchecked")
        public Delegate(DHTValueBag bag, Collection<? extends E> delegate) {
            this.bag = bag;
            this.view = (Collection<E>)delegate;
        }
        
        public boolean add(E o) {
            boolean wasEmpty = isEmpty();
            boolean added = view.add(o);
            if (added && wasEmpty && !isEmpty()) {
                database.put(bag.valueId, bag);
            }
            return added;
        }

        public boolean addAll(Collection<? extends E> c) {
            boolean wasEmpty = isEmpty();
            boolean added = view.addAll(c);
            if (added && wasEmpty && !isEmpty()) {
                database.put(bag.valueId, bag);
            }
            return added;
        }

        public void clear() {
            boolean wasEmpty = isEmpty();
            view.clear();
            if (!wasEmpty && isEmpty()) {
                DHTValueBag r = database.remove(bag.valueId);
                // Checking for null just in case Sun is
                // changing the impl. of the views
                assert (r == null || r == bag);
            }
        }

        public boolean contains(Object o) {
            return view.contains(o);
        }

        public boolean containsAll(Collection<?> c) {
            return view.containsAll(c);
        }

        public boolean isEmpty() {
            return view.isEmpty();
        }

        public Iterator<E> iterator() {
            return view.iterator();
        }

        public boolean remove(Object o) {
            boolean removed = view.remove(o);
            if (removed && isEmpty()) {
                DHTValueBag r = database.remove(bag.valueId);
                // Checking for null just in case Sun is
                // changing the impl. of the views
                assert (r == null || r == bag);
            }
            return removed;
        }

        public boolean removeAll(Collection<?> c) {
            boolean removed = view.removeAll(c);
            if (removed && isEmpty()) {
                DHTValueBag r = database.remove(bag.valueId);
                // Checking for null just in case Sun is
                // changing the impl. of the views
                assert (r == null || r == bag);
            }
            return removed;
        }

        public boolean retainAll(Collection<?> c) {
            boolean retained = view.retainAll(c);
            if (retained && isEmpty()) {
                DHTValueBag r = database.remove(bag.valueId);
                // Checking for null just in case Sun is
                // changing the impl. of the views
                assert (r == null || r == bag);
            }
            return retained;
        }

        public int size() {
            return view.size();
        }

        public Object[] toArray() {
            return view.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return view.toArray(a);
        }
    }
}
