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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.limegroup.mojito.util.Trie.Cursor;

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
            public boolean select(Entry<K, V> entry) {
                if (cursor == null || cursor.select(entry)) {
                    values.add(entry.getValue());
                }
                return values.size() >= size;
            }
        });
        
        return values;
    }
    
    public static <K,V> Set<Map.Entry<K, V>> entrySet(Trie<K,V> trie) {
        return entrySet(trie, new HashSet<Map.Entry<K, V>>(trie.size()));
    }
    
    public static <K,V> Set<Map.Entry<K, V>> entrySet(Trie<K,V> trie, final Set<Map.Entry<K, V>> entries) {
        trie.traverse(new Cursor<K, V>() {
            public boolean select(Entry<K, V> entry) {
                entries.add(entry);
                return false;
            }
        });
        return entries;
    }

    public static <K,V> Set<K> keySet(Trie<K,V> trie) {
        return keySet(trie, new HashSet<K>(trie.size()));
    }
    
    public static <K,V> Set<K> keySet(Trie<K,V> trie, final Set<K> keys) {
        trie.traverse(new Cursor<K, V>() {
            public boolean select(Entry<K, V> entry) {
                keys.add(entry.getKey());
                return false;
            }
        });
        return keys;
    }
    
    public static <K,V> Collection<V> values(Trie<K,V> trie) {
        return values(trie, new ArrayList<V>(trie.size()));
    }
    
    public static <K,V> Collection<V> values(Trie<K,V> trie, final Collection<V> values) {
        trie.traverse(new Cursor<K, V>() {
            public boolean select(Entry<K, V> entry) {
                values.add(entry.getValue());
                return false;
            }
        });
        return values;
    }
    
    public static <K,V> Trie<K,V> synchronizedTrie(Trie<K,V> trie) {
        return new SynchronizedTrie<K,V>(trie);
    }
    
    private static class SynchronizedTrie<K,V> implements Trie<K,V>, Serializable {

        private static final long serialVersionUID = -8553889709551579766L;

        private Trie<K,V> trie;
        
        private SynchronizedTrie(Trie<K,V> trie) {
            this.trie = trie;
        }
        
        public synchronized void clear() {
            trie.clear();
        }

        public synchronized boolean containsKey(K key) {
            return trie.containsKey(key);
        }

        public synchronized V get(K key) {
            return trie.get(key);
        }

        public synchronized boolean isEmpty() {
            return trie.isEmpty();
        }

        public synchronized List<K> keys() {
            return trie.keys();
        }

        public synchronized V put(K key, V value) {
            return trie.put(key, value);
        }

        public synchronized List<V> range(K key, int length, Cursor<K,V> cursor) {
            return trie.range(key, length, cursor);
        }

        public synchronized List<V> range(K key, int length) {
            return trie.range(key, length);
        }

        public synchronized V remove(K key) {
            return trie.remove(key);
        }

        public synchronized Map.Entry<K,V> select(K key, Cursor<K,V> cursor) {
            return trie.select(key, cursor);
        }
        
        public synchronized V select(K key) {
            return trie.select(key);
        }

        public synchronized Map.Entry<K,V> traverse(Cursor<K,V> cursor) {
            return trie.traverse(cursor);
        }

        public synchronized int size() {
            return trie.size();
        }

        public synchronized List<V> values() {
            return trie.values();
        }
        
        public synchronized String toString() {
            return trie.toString();
        }
    }
}
