/*
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

import java.util.Map;
import java.util.SortedMap;

/**
 * An interface for Tries
 * 
 * @author Roger Kapsi
 * @author Sam Berlin
 */
public interface Trie<K, V> extends SortedMap<K, V> {
    
    /**
     * Returns a view of this Trie of all elements that are
     * prefixed by the given key.
     * 
     * In a fixed-keysize Trie, this is essentially a 'get' operation.
     * 
     * For example, if the trie contains 'Lime', 'LimeWire', 
     * 'LimeRadio', 'Lax', 'Later', 'Lake', and 'Lovely', then
     * a lookup of 'Lime' would return 'Lime', 'LimeRadio', and 'LimeWire'.
     * 
     * @param key
     * @return
     */
    public SortedMap<K, V> getPrefixedBy(K key);
    
    /**
     * Returns a view of this Trie of all elements that are
     * prefixed by the length of the key.
     * 
     * Fixed-keysize Tries will not support this operation
     * (because all keys will be the same length).
     * 
     * For example, if the trie contains 'Lime', 'LimeWire', 
     * 'LimeRadio', 'Lax', 'Later', 'Lake', and 'Lovely', then
     * a lookup of 'LimePlastics' with a length of 4 would
     * return 'Lime', 'LimeRadio', and 'LimeWire'.
     *  
     * @param key
     * @param length
     * @return
     */
    public SortedMap<K, V> getPrefixedBy(K key, int length);
    
    /**
     * Returns a view of this Trie of all elements that are prefixed
     * by the key, starting at the given offset and for the given length.
     * 
     * Fixed-keysize Tries will not support this operation
     * (because all keys are the same length).
     *
     * For example, if the trie contains 'Lime', 'LimeWire', 
     * 'LimeRadio', 'Lax', 'Later', 'Lake', and 'Lovely', then
     * a lookup of 'The Lime Plastics' with an offset of 4 and a 
     * length of 4 would return 'Lime', 'LimeRadio', and 'LimeWire'.
     * 
     * @param key
     * @param offset
     * @param length
     * @return
     */
    public SortedMap<K, V> getPrefixedBy(K key, int offset, int length);
    
    /**
     * Returns a view of this Trie of all elements that are prefixed
     * by the number of bits in the given Key.
     * 
     * Fixed-keysize Tries can support this operation as a way to do
     * lookups of partial keys.  That is, if the Trie is storing IP
     * addresses, you can lookup all addresses that begin with
     * '192.168' by providing the key '192.168.X.X' and a length of 16
     * would return all addresses that begin with '192.168'.
     * 
     * @param key
     * @param length
     * @return
     */
    public SortedMap<K, V> getPrefixedByBits(K key, int bitLength);
    
    /**
     * Returns the value for the entry whose key is closest in a bitwise
     * XOR metric to the given key.  This is NOT lexographic closeness.
     * For example, given the keys:
     *  D = 1000100
     *  H = 1001000
     *  L = 1001100
     * If the trie contained 'H' and 'L', a lookup of 'D' would return 'L',
     * because the XOR between D & L is closer than the XOR between D & H. 
     *  
     * @param key
     * @return
     */
    public V select(K key);
    
    /**
     * Iterates through the trie, starting with the entry whose bitwise
     * value is closest in an XOR metric to the given key.  After the closest
     * entry is found, the trie will call select on that entry and continue
     * calling select for each entry (traversing in order of XOR closeness,
     * NOT lexographically) until the cursor returns Cursor.SelectStatus.EXIT.
     * The cursor can return Cursor.SelectStatus.CONTINUE to continue traversing.
     * Cursor.SelectStatus.REMOVE_AND_EXIT is used to remove the current element
     * and stop traversing.
     * 
     * The Cursor.SelectStatus.REMOVE operation is not supported.
     * 
     * @param key
     * @param cursor
     * @return The entry the cursor returned EXIT on, or null if it continued
     *         till the end.
     */
    public Map.Entry<K,V> select(K key, Cursor<? super K, ? super V> cursor);
    
    /**
     * Traverses the trie in lexographic order.  Cursor.select will be called
     * on each entry.  The traversal will stop when the cursor returns
     * Cursor.SelectStatus.EXIT.  Cursor.SelectStatus.CONTINUE is used to 
     * continue traversing.  Cursor.SelectStatus.REMOVE is used to remove
     * the element that was selected and continue traversing.
     * Cursor.SelectStatus.REMOVE_AND_EXIT is used to remove the current element
     * and stop traversing.
     *   
     * @param cursor
     * @return The entry the cursor returned EXIT on, or null if it continued
     *         till the end.
     */
    public Map.Entry<K,V> traverse(Cursor<? super K, ? super V> cursor);
    
    /**
     * Allows operations to be performed given entries of the Trie as
     * it is traversing through the entries.
     *
     * @param <K>
     * @param <V>
     */
    public static interface Cursor<K, V> {
        
        /**
         * Notification that the trie is currently looking at the given entry.
         * Return EXIT to finish the trie operation, CONTINUE to look at the
         * next entry, REMOVE to remove the entry and continue iterating, or
         * REMOVE_AND_EXIT to remove the entry and stop iterating. 
         * Not all operations support REMOVE.
         * 
         * @param entry
         * @return
         */
        public SelectStatus select(Map.Entry<? extends K, ? extends V> entry);
     
        public static enum SelectStatus {
            EXIT, CONTINUE, REMOVE, REMOVE_AND_EXIT;
        }
    }
}