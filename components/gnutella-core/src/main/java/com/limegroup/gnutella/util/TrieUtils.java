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
 
package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.limegroup.gnutella.util.Trie.Cursor;

/**
 * Miscellaneous utilities for Tries
 */
public final class TrieUtils {
    
    private TrieUtils() {}
    
    public static <K,V> List<V> select(Trie<K,V> trie, K key, int count) {
        return select(trie, key, count, null);
    }
    
    public static <K,V> List<V> select(Trie<K,V> trie, K key, 
                int count, final Cursor<K,V> cursor) {
        
        final int size = Math.min(trie.size(), count);
        final List<V> values = new ArrayList<V>(size);
        
        trie.select(key, new Cursor<K,V>() {
            public boolean select(Entry<? extends K, ? extends V> entry) {
                if (cursor == null || cursor.select(entry)) {
                    values.add(entry.getValue());
                }
                return values.size() >= size;
            }
        });
        
        return values;
    }
}
