/*
 * Mojito Distributed Hash Tabe (DHT)
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
 
package com.limegroup.mojito.util;

import java.util.Collection;
import java.util.Iterator;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.messages.FindValueResponse;

/**
 * A collection of KeyValues
 */
public final class KeyValueCollection implements Collection<KeyValue> {
    
    private static final long serialVersionUID = 3740150886075929145L;
    
    private Contact source;
    private Collection<KeyValue> keyValues;
    
    public KeyValueCollection(FindValueResponse response) {
        this(response.getContact(), response.getValues());
    }
    
    public KeyValueCollection(Contact source, Collection<KeyValue> keyValues) {
        this.source = source;
        this.keyValues = keyValues;
    }
    
    public Contact getSource() {
        return source;
    }

    public boolean add(KeyValue o) {
        throw new UnsupportedOperationException("This class immutable");
    }

    public boolean addAll(Collection<? extends KeyValue> c) {
        throw new UnsupportedOperationException("This class immutable");
    }

    public void clear() {
        throw new UnsupportedOperationException("This class immutable");
    }

    public boolean contains(Object o) {
        return keyValues.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return keyValues.containsAll(c);
    }

    public boolean isEmpty() {
        return keyValues.isEmpty();
    }

    public Iterator<KeyValue> iterator() {
        return keyValues.iterator();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("This class immutable");
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("This class immutable");
    }

    public boolean retainAll(Collection<?> c) {
        return keyValues.removeAll(c);
    }

    public int size() {
        return keyValues.size();
    }

    public Object[] toArray() {
        return keyValues.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return keyValues.toArray(a);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Source: ").append(getSource()).append("\n");
        buffer.append(CollectionUtils.toString(keyValues));
        return buffer.toString();
    }
}
