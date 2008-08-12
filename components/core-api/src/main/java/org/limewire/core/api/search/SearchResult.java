package org.limewire.core.api.search;

import java.util.List;
import java.util.Map;

import org.limewire.core.api.endpoint.RemoteHost;

public interface SearchResult {

    enum PropertyKey {
        ALBUM_TITLE, ARTIST_NAME, COMMENT, QUALITY, TRACK_NAME, TRACK_TIME
    }

    String getUrn();

    Map<PropertyKey, Object> getProperties();
    
    ResultType getResultType();
    
    String getDescription();
    
    long getSize();
    
    String getFileExtension();
    
    List<RemoteHost> getSources();

}
