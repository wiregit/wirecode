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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.limewire.mojito.routing.Bucket;


/**
 * Miscellaneous utilities for Buckets
 */
public final class BucketUtils {
    
    /**
     * A Comparator that orders a Collection of Buckets by their
     * depth in a Tree.
     */
    public static final Comparator<Bucket> BUCKET_DEPTH_COMPARATOR = new Comparator<Bucket>() {
        public int compare(Bucket o1, Bucket o2) {
            return o1.getDepth() - o2.getDepth();
        }
    };
    
    private BucketUtils() {}
    
    /**
     * Sort this list of Buckets by depth. Used for things such as 
     * building a binary tree out of this list of buckets.
     */
    public static <T extends Bucket> List<T> sortByDepth(List<T> buckets){
        Collections.sort(buckets, BUCKET_DEPTH_COMPARATOR);
        return buckets;
    }
}
