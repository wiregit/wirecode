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
        COMPANY,
        DATE_CREATED,
        FILE_SIZE, // in megabytes
        FILES_IN_ARCHIVE, // # of files in an archive file (zip, tar, ...)
        GENRE,
        HEIGHT,
        LENGTH, // in time (hh:mm:ss)
        NAME,
        OWNER,
        PLATFORM,
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

    void addSimilarResult(SearchResult result);

    String getDescription();
    
    String getFileExtension();
    
    Map<PropertyKey, Object> getProperties();

    Object getProperty(PropertyKey key);
    
    ResultType getResultType();
    
    List<SearchResult> getSimiliarResults();

    long getSize();
    
    List<RemoteHost> getSources();

    String getUrn();
}