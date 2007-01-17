/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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
 
package org.limewire.mojito.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Miscellaneous utilities for Collections
 */
public final class CollectionUtils {
    
    private CollectionUtils() {}
    
    /**
     * Converts the given Collection to a Set (if it isn't
     * already a Set)
     */
    public static <T> Set<T> toSet(Collection<T> c) {
        if (c instanceof Set) {
            return (Set<T>)c;
        }
        
        return new LinkedHashSet<T>(c);
    }
    
    /**
     * Returns the given Collection as formatted String
     */
    public static String toString(Collection<?> c) {
        StringBuilder buffer = new StringBuilder();
        
        Iterator it = c.iterator();
        for(int i = 0; it.hasNext(); i++) {
            buffer.append(i).append(": ").append(it.next()).append('\n');
        }
        
        // Delete the last \n
        if(buffer.length() > 1) {
            buffer.setLength(buffer.length()-1);
        }
        return buffer.toString();
    }
}
