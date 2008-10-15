package org.limewire.core.impl.library;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.RemoteFileDesc;

public class CoreRemoteFileItem implements RemoteFileItem {
    private final RemoteFileDescAdapter searchResult;
    private final Map<Keys, Object> map;

    CoreRemoteFileItem(RemoteFileDescAdapter rfd) {
        this.searchResult = rfd;
        this.map = Collections.synchronizedMap(new HashMap<Keys,Object>());
    }
    
    public RemoteFileDesc getRfd() {
        return searchResult.getRfd();
    }
    
    public SearchResult getSearchResult() {
        return searchResult;
    }
    
    public String getName() {
        return (String)searchResult.getProperty(SearchResult.PropertyKey.NAME);
    }
    
    public String getFileName(){
        return getRfd().getFileName();
    }

    public long getSize() {
        return searchResult.getSize();
    }

    public long getCreationTime() {
        Long time = (Long)searchResult.getProperty(SearchResult.PropertyKey.DATE_CREATED);
        if(time == null)
            return -1;
        else
            return time;
    }

    public long getLastModifiedTime() {
        return 0;  // TODO
    }

    public int getNumHits() {
        return 0;  // TODO
    }

    public int getNumUploads() {
        return 0;  // TODO
    }

    public Category getCategory() {
        return searchResult.getCategory();   
    }

    public Object getProperty(Keys key) {
        return map.get(key);
    }
    
    @Override
    public void setProperty(Keys key, Object value) {
        map.put(key, value);
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    @Override
    public URN getUrn() {
        return searchResult.getUrn();
    }
}
