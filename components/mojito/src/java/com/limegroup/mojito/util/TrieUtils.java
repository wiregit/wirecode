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
import java.util.List;

/**
 * Miscellaneous utilities for Tries
 */
public final class TrieUtils {
    
    private TrieUtils() {}
    
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

        public synchronized List<V> range(K key, int length, KeySelector<K,V> selector) {
            return trie.range(key, length, selector);
        }

        public synchronized List<V> range(K key, int length) {
            return trie.range(key, length);
        }

        public synchronized V remove(K key) {
            return trie.remove(key);
        }

        public synchronized List<V> select(K key, int count, KeySelector<K,V> selector) {
            return trie.select(key, count, selector);
        }

        public synchronized List<V> select(K key, int count) {
            return trie.select(key, count);
        }

        public synchronized V select(K key) {
            return trie.select(key);
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
