package com.limegroup.mojito.routing.impl;

import java.util.Collection;
import java.util.List;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;

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

    public List<? extends Contact> select(KUID nodeId, int count);

    public boolean remove(KUID nodeId);

    public boolean removeLive(KUID nodeId);

    public boolean removeCache(KUID nodeId);

    public boolean contains(KUID nodeId);

    public boolean containsLive(KUID nodeId);

    public boolean containsCache(KUID nodeId);

    public boolean isLiveFull();

    public boolean isCacheFull();

    public boolean isTooDeep();

    public Collection<? extends Contact> live();

    public Collection<? extends Contact> cache();

    public Contact getLeastRecentlySeenLiveNode();

    public Contact getMostRecentlySeenLiveNode();

    // O(1)
    public Contact getLeastRecentlySeenCachedNode();

    // O(n)
    public Contact getMostRecentlySeenCachedNode();

    public List<? extends Bucket> split();

    public int size();

    public int getLiveSize();

    public int getCacheSize();

    public void clear();
}