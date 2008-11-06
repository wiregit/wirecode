package org.limewire.core.impl.library;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.RemoteFileDesc;

public class CoreRemoteFileItem implements RemoteFileItem {
    private final RemoteFileDescAdapter searchResult;

    CoreRemoteFileItem(RemoteFileDescAdapter rfd) {
        this.searchResult = rfd;
    }
    
    public RemoteFileDesc getRfd() {
        return searchResult.getRfd();
    }
    
    public SearchResult getSearchResult() {
        return searchResult;
    }
    
    public String getName() {
        return (String)searchResult.getProperty(FilePropertyKey.NAME);
    }
    
    public String getFileName(){
        return getRfd().getFileName();
    }

    public long getSize() {
        return searchResult.getSize();
    }

    public long getCreationTime() {
        Long time = (Long)searchResult.getProperty(FilePropertyKey.DATE_CREATED);
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

    public Object getProperty(FilePropertyKey key) {
        return searchResult.getProperty(key);
    }
    
    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            String stringValue = value.toString();
            return stringValue;
        } else {
            return null;
        }
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
