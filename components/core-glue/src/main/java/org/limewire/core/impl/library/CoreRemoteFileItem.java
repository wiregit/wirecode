package org.limewire.core.impl.library;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.SearchResult;
import org.limewire.util.MediaType;

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
        return ((Calendar)searchResult.getProperty(SearchResult.PropertyKey.DATE_CREATED)).getTime().getTime();
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
        MediaType type = MediaType.getMediaTypeForExtension(searchResult.getFileExtension());
        if (type == MediaType.getAudioMediaType()) {
            return Category.AUDIO;
        } else if (type == MediaType.getVideoMediaType()) {
            return Category.VIDEO;
        } else if (type == MediaType.getImageMediaType()) {
            return Category.IMAGE;
        } else if (type == MediaType.getDocumentMediaType()) {
            return Category.DOCUMENT;
        } else if (type == MediaType.getProgramMediaType()) {
            return Category.PROGRAM;
        }
        return Category.OTHER;   
    }

    public Object getProperty(Keys key) {
        return map.get(key);
    }
    
    @Override
    public void setProperty(Keys key, Object value) {
        map.put(key, value);
    }
}
