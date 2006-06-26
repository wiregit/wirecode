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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.limegroup.mojito.Contact;

/**
 * Miscellaneous utilities for Buckets
 */
public final class BucketUtils {
    
    private BucketUtils() {}
    
    /**
     * Returns the most recently seen contact from the list.
     * Use BucketUtils.sort() prior to calling this Method!
     */
    public static Contact getMostRecentlySeen(List<? extends Contact> nodes) {
        return nodes.get(0);
    }
    
    /**
     * Returns the least recently seen contact from the list.
     * Use BucketUtils.sort() prior to calling this Method!
     */
    public static Contact getLeastRecentlySeen(List<? extends Contact> nodes) {
        return nodes.get(nodes.size()-1);
    }
    
    /**
     * Sorts the contacts from most recently seen to
     * least recently seen
     */
    public static List<? extends Contact> sort(List<? extends Contact> nodes) {
        Collections.sort(nodes, new Comparator<Contact>() {
            public int compare(Contact a, Contact b) {
                long t1 = a.getTimeStamp();
                long t2 = b.getTimeStamp();
                if (t1 == t2) {
                    return 0;
                } else if (t1 > t2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return nodes;
    }
    
    /**
     * Sorts the contacts from most recently seen to
     * least recently seen based on their <tt>LastDeadOrAliveTime</tt>.
     * 
     * Used when loading the routing table
     */
    public static List<? extends Contact> sortLastDeadOrAlive(List<? extends Contact> nodes) {
        Collections.sort(nodes, new Comparator<Contact>() {
            public int compare(Contact a, Contact b) {
                long t1 = a.getLastDeadOrAliveTime();
                long t2 = b.getLastDeadOrAliveTime();
                if (!a.hasFailed() && !b.hasFailed()) {
                    if (t1 == t2) {
                        return 0;
                    } else if (t1 > t2) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else if (a.hasFailed() && !b.hasFailed()) {
                    return 1;
                } else if (!a.hasFailed() && b.hasFailed()) {
                    return -1;
                } else {
                    if (t1 == t2) {
                        return 0;
                    } else if (t1 > t2) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
        });
        return nodes;
    }
}
