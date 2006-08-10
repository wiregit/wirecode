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
 
package com.limegroup.mojito.routing.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;

/**
 * An interface for Buckets
 */
public interface Bucket extends Serializable {

    /**
     * Returns the Bucket KUID
     */
    public KUID getBucketID();

    /**
     * Returns the depth of the Bucket in the Trie
     */
    public int getDepth();

    /**
     * Set the time stamp of the Bucket to 'now'
     */
    public void touch();

    /**
     * Returns the time stamp when this Bucket was refreshed
     * last time
     */
    public long getTimeStamp();

    /**
     * Adds the Contact as a live contact (i.e. it will 
     * be actively used for routing).
     */
    public void addLiveContact(Contact node);

    /**
     * Add the Contact to the replacement cache.
     */
    public void addCachedContact(Contact node);

    /**
     * Updates the Contact in this bucket 
     */
    public Contact updateContact(Contact node);
    
    /**
     * Returns the Contact that has the provided KUID.
     */
    public Contact get(KUID nodeId);

    /**
     * Returns the Contact that has the provided KUID.
     */
    public Contact getLiveContact(KUID nodeId);

    /**
     * Returns the Contact that has the provided KUID.
     */
    public Contact getCachedContact(KUID nodeId);

    /**
     * Returns the best matching Contact for the provided KUID
     */
    public Contact select(KUID nodeId);

    /**
     * Returns the 'count' best matching Contacts for the provided KUID
     */
    public List<Contact> select(KUID nodeId, int count);

    /**
     * Removes the Contact that has the provided KUID
     */
    public boolean remove(KUID nodeId);

    /**
     * Removes the Contact that has the provided KUID
     */
    public boolean removeLiveContact(KUID nodeId);

    /**
     * Removes the Contact that has the provided KUID
     */
    public boolean removeCachedContact(KUID nodeId);

    /**
     * Returns whether or not this Bucket contains a Contact with this KUID
     */
    public boolean contains(KUID nodeId);

    /**
     * Returns whether or not this Bucket contains a Contact with this KUID
     */
    public boolean containsLiveContact(KUID nodeId);

    /**
     * Returns whether or not this Bucket contains a Contact with this KUID
     */
    public boolean containsCachedContact(KUID nodeId);

    /**
     * Returns whether or not this Bucket is full
     */
    public boolean isLiveFull();

    /**
     * Returns whether or not this Bucket is full
     */
    public boolean isCacheFull();

    /**
     * Returns whether or not this Bucket is too deep in the Trie
     */
    public boolean isTooDeep();

    /**
     * Returns all live Contacts as List
     */
    public Collection<Contact> getLiveContacts();

    /**
     * Returns all cached Contacts as List
     */
    public Collection<Contact> getCachedContacts();

    /**
     * Returns the least recently seen live Contact
     */
    public Contact getLeastRecentlySeenLiveContact();

    /**
     * Returns the most recently seen live Contact
     */
    public Contact getMostRecentlySeenLiveContact();

    /**
     * Returns the least recently seen cached Contact
     */
    public Contact getLeastRecentlySeenCachedContact();

    /**
     * Returns the most recently seen cached Contact
     */
    public Contact getMostRecentlySeenCachedContact();
    
    /**
     * Removes the unknown and dead Contacts in this bucket.
     */
    public void purge();

    /**
     * Splits the Bucket into two parts
     */
    public List<Bucket> split();

    /**
     * Returns the total number of Contacts in the Bucket
     */
    public int size();

    /**
     * Returns the number of live Contacts in the Bucket
     */
    public int getLiveSize();
    
    /**
     * Returns the number of cached Contacts in the Bucket
     */
    public int getCacheSize();

    /**
     * Clears the Bucket
     */
    public void clear();
    
    /**
     * Returns whether or not this Bucket needs to be refreshed
     */
    public boolean isRefreshRequired();
}
