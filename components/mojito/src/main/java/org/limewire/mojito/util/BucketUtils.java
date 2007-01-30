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
import org.limewire.mojito.routing.Contact;


/**
 * Miscellaneous utilities for Buckets
 */
public final class BucketUtils {
    
    /**
     * A helper method to compare longs.
     */
    private static int compareLong(long a, long b) {
        if (a < b) {
            return -1;
        } else if (a > b) {
            return 1;
        } else {
            return 0;
        }
    }
    
    /**
     * A Comparator that orders a Collection of Contacts from
     * most recently seen to least recently seen.
     */
    public static final Comparator<Contact> CONTACT_MRS_COMPARATOR = new Comparator<Contact>() {
        public int compare(Contact a, Contact b) {
            // Note: There's a minus sign to change the order from
            // 'small to big' to 'big to small' values
            return -compareLong(a.getTimeStamp(), b.getTimeStamp());
        }
    };
    
    /**
     * A Comparator that orders a Collection of Buckets by their
     * depth in a Tree.
     */
    public static final Comparator<Bucket> BUCKET_DEPTH_COMPARATOR = new Comparator<Bucket>() {
        public int compare(Bucket o1, Bucket o2) {
            return o1.getDepth() - o2.getDepth();
        }
    };
    
    /**
     * A Comparator that orders a Collection of Contacts from alive
     * to failed. The sub-set of alive Contacts is ordered from most
     * recently seen to least recently seen and the sub-set of failed
     * Contacts is ordered by least recently failed to most recently
     * failed.
     */
    public static final Comparator<Contact> CONTACT_ALIVE_TO_FAILED_COMPARATOR = new Comparator<Contact>() {
        public int compare(Contact a, Contact b) {
            // If neither a not b has failed then use the standard
            // most recently seen (MRS) comparator
            if (!a.hasFailed() && !b.hasFailed()) {
                return CONTACT_MRS_COMPARATOR.compare(a, b);
            
            // If a has failed and b hasn't then move a to the
            // end of the collection
            } else if (a.hasFailed() && !b.hasFailed()) {
                return 1;
            
            // If a hasn't failed and b has then move b to
            // the end of the collection
            } else if (!a.hasFailed() && b.hasFailed()) {
                return -1;
            
            // If both have failed then order by least recently 
            // failed to most recently failed
            } else { 
                return compareLong(a.getLastFailedTime(), b.getLastFailedTime());
            }
        }
    };
    
    private BucketUtils() {}
    
    /**
     * Returns the most recently seen contact from the list.
     * Use BucketUtils.sort() prior to calling this Method!
     */
    public static <T extends Contact> Contact getMostRecentlySeen(List<T> nodes) {
        assert (nodes.get(0).getTimeStamp() >= nodes.get(nodes.size()-1).getTimeStamp());
        return nodes.get(0);
    }
    
    /**
     * Returns the least recently seen contact from the list.
     * Use BucketUtils.sort() prior to calling this Method!
     */
    public static <T extends Contact> Contact getLeastRecentlySeen(List<T> nodes) {
        assert (nodes.get(nodes.size()-1).getTimeStamp() <= nodes.get(0).getTimeStamp());
        return nodes.get(nodes.size()-1);
    }
    
    /**
     * Sorts the given List of Contacts from most recently seen to 
     * least recently seen.
     */
    public static <T extends Contact> List<T> getMostRecentlySeenContacts(List<T> nodes) {
        return sort(nodes);
    }
    
    /**
     * Sorts the given List of Contacts from most recently seen to 
     * least recently seen and returns a sub-list with at most
     * count number of elements.
     */
    public static <T extends Contact> List<T> getMostRecentlySeenContacts(List<T> nodes, int count) {
        return sort(nodes).subList(0, Math.min(count, nodes.size()));
    }
    
    /**
     * Sorts the Contacts from most recently seen to
     * least recently seen
     */
    public static <T extends Contact> List<T> sort(List<T> nodes) {
        Collections.sort(nodes, CONTACT_MRS_COMPARATOR);
        return nodes;
    }
    
    /**
     * Sorts the contacts from most recently seen to
     * least recently seen based on their timestamp and last failed time.
     * 
     * Used when loading the routing table if our nodeID has changed
     */
    public static <T extends Contact> List<T> sortAliveToFailed(List<T> nodes) {
        Collections.sort(nodes, CONTACT_ALIVE_TO_FAILED_COMPARATOR);
        return nodes;
    }
    
    /**
     * Sort this list of Buckets by depth. Used for things such as 
     * building a binary tree out of this list of buckets.
     */
    public static <T extends Bucket> List<T> sortByDepth(List<T> buckets){
        Collections.sort(buckets, BUCKET_DEPTH_COMPARATOR);
        return buckets;
    }
}
