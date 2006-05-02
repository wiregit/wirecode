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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.kapsi.net.kademlia.ContactNode;

public final class BucketUtils {
    
    private BucketUtils() {}
    
    /**
     * Returns the most recently seen contact from the list.
     * Use BucketUtils.sort() prior to calling this Method!
     */
    public static ContactNode getMostRecentlySeen(List bucketList) {
        return (ContactNode)bucketList.get(0);
    }
    
    /**
     * Returns the least recently seen contact from the list.
     * Use BucketUtils.sort() prior to calling this Method!
     */
    public static ContactNode getLeastRecentlySeen(List bucketList) {
        return (ContactNode)bucketList.get(bucketList.size()-1);
    }
    
    /**
     * Sorts the contacts from most recently seen to
     * least recently seen
     */
    public static List sort(List bucketList) {
        Collections.sort(bucketList, new Comparator() {
            public int compare(Object a, Object b) {
                long t1 = ((ContactNode)a).getTimeStamp();
                long t2 = ((ContactNode)b).getTimeStamp();
                if (t1 == t2) {
                    return 0;
                } else if (t1 > t2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return bucketList;
    }
    
    /**
     * Sorts the contacts from most recently seen to
     * least recently seen based on their <tt>LastDeadOrAliveTime</tt>.
     * 
     * Used when loading the routing table
     */
    public static List sortLastDeadOrAlive(List nodesList) {
        Collections.sort(nodesList, new Comparator() {
            public int compare(Object a, Object b) {
                ContactNode n1 = (ContactNode)a;
                ContactNode n2 = (ContactNode)b;
                long t1 = n1.getLastDeadOrAliveTime();
                long t2 = n2.getLastDeadOrAliveTime();
                if(!n1.hasFailed() && !n2.hasFailed()) {
                    if (t1 == t2) {
                        return 0;
                    } else if (t1 > t2) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else if(n1.hasFailed() && !n2.hasFailed()) {
                    return 1;
                } else if (!n1.hasFailed() && n2.hasFailed()) {
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
        return nodesList;
    }
}
