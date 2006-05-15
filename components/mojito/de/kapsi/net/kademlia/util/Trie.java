/*
 * Lime Kademlia Distributed Hash Table (DHT)
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

package de.kapsi.net.kademlia.util;

import java.util.List;

public interface Trie {
    
    public Object put(Object key, Object value);
    public Object get(Object key);
    public Object remove(Object key);
    public boolean containsKey(Object key);
    //public boolean containsValue(Object value);
    public int size();
    public boolean isEmpty();
    public void clear();
    
    public List keys();
    public List values();
    
    public List range(Object key, int length);
    public List range(Object key, int length, KeySelector selector);
    
    public Object select(Object key);
    public List select(Object key, int count);
    public List select(Object key, int count, KeySelector selector);
    
    public static interface KeySelector {
        public boolean allow(Object key, Object value);
    }
}
