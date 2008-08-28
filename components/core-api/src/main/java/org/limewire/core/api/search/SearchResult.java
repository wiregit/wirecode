package org.limewire.core.api.search;

import java.util.List;
import java.util.Map;

import org.limewire.core.api.endpoint.RemoteHost;

public interface SearchResult {

    enum PropertyKey {
        ALBUM_TITLE,
        ARTIST_NAME,
        AUTHOR,
        BITRATE, // ex. 128, 160, 192, 224, 320
        COMMENTS,
        DATE_CREATED,
        GENRE,
        HEIGHT,
        LENGTH,
        NAME,
        OWNER,
        QUALITY,
        RATING,
        RELEVANCE,
        SAMPLE_RATE, // ex. 44,100 Hz
        // get size from VisualSearchResult.getSize()
        TRACK_NUMBER,
        // get type from VisualSearchResult.getFileExtension()
        WIDTH,
        YEAR
    }

    String getUrn();

    Map<PropertyKey, Object> getProperties();

    Object getProperty(PropertyKey key);
    
    ResultType getResultType();
    
    String getDescription();
    
    long getSize();
    
    String getFileExtension();
    
    List<RemoteHost> getSources();
}