package org.limewire.core.impl.library;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.SearchResult;

public class CoreRemoteFileItem implements RemoteFileItem {
    private final SearchResult searchResult;
    private final Map<Keys, Object> map;

    CoreRemoteFileItem(SearchResult searchResult) {
        this.searchResult = searchResult;
        this.map = Collections.synchronizedMap(new HashMap<Keys,Object>());
    }
    
    public String getName() {
        return (String)searchResult.getProperty(SearchResult.PropertyKey.NAME);
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
}
