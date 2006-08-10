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
 
package com.limegroup.mojito.util;

import java.util.List;
import java.util.Map;

/**
 * An interface for Tries
 */
public interface Trie<K, V> {
    
    public V put(K key, V value);
    public V get(K key);
    public V remove(K key);
    public boolean containsKey(K key);
    //public boolean containsValue(V value);
    public int size();
    public boolean isEmpty();
    public void clear();
    
    public List<K> keys();
    public List<V> values();
    
    public List<V> range(K key, int length);
    public List<V> range(K key, int length, Cursor<? super K, ? super V> cursor);
    
    public V select(K key);
    public Map.Entry<K,V> select(K key, Cursor<? super K, ? super V> cursor);
    
    public Map.Entry<K,V> traverse(Cursor<? super K, ? super V> cursor);
    
    public static interface Cursor<K, V> {
        public boolean select(Map.Entry<? extends K, ? extends V> entry);
    }
}
