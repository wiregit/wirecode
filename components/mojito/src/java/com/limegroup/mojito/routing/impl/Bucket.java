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

package com.limegroup.mojito.routing.impl;

import java.util.Collection;
import java.util.List;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;

/**
 * 
 */
interface Bucket {

    public KUID getBucketID();

    public int getDepth();

    public void touch();

    public long getTimeStamp();

    public void addLive(Contact node);

    public void addCache(Contact node);

    public Contact get(KUID nodeId);

    public Contact getLive(KUID nodeId);

    public Contact getCache(KUID nodeId);

    public Contact select(KUID nodeId);

    public List<Contact> select(KUID nodeId, int count);

    public boolean remove(KUID nodeId);

    public boolean removeLive(KUID nodeId);

    public boolean removeCache(KUID nodeId);

    public boolean contains(KUID nodeId);

    public boolean containsLive(KUID nodeId);

    public boolean containsCache(KUID nodeId);

    public boolean isLiveFull();

    public boolean isCacheFull();

    public boolean isTooDeep();

    public Collection<Contact> live();

    public Collection<Contact> cache();

    public Contact getLeastRecentlySeenLiveContact();

    public Contact getMostRecentlySeenLiveContact();

    public Contact getLeastRecentlySeenCachedContact();

    public Contact getMostRecentlySeenCachedContact();

    public List<Bucket> split();

    public int size();

    public int getLiveSize();
    
    public int getCacheSize();

    public void clear();
    
    public boolean isRefreshRequired();
}