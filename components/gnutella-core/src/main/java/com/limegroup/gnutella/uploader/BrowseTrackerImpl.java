package com.limegroup.gnutella.uploader;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.api.friend.Friend;

import com.google.inject.Singleton;

@Singleton
public class BrowseTrackerImpl implements BrowseTracker {
    
    private final Map<String, Date> browseHistory;
    private final Map<String, Date> refreshHistory;
    
    public BrowseTrackerImpl() {
        this.browseHistory = new ConcurrentHashMap<String, Date>();
        this.refreshHistory = new ConcurrentHashMap<String, Date>();
    }
    
    @Override
    public void browsed(Friend friend) {
        browseHistory.put(friend.getId(), new Date());    
    }

    @Override
    public Date lastBrowseTime(Friend friend) {
        return browseHistory.get(friend.getId());
    }

    @Override
    public void sentRefresh(Friend friend) {
        refreshHistory.put(friend.getId(), new Date());
    }

    @Override
    public Date lastRefreshTime(Friend friend) {
        return refreshHistory.get(friend.getId());
    }
}
