package org.limewire.core.api.search;

import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;

public interface SearchResult {

  public enum PropertyKey {
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
        TRACK_NAME,
        // get type from VisualSearchResult.getFileExtension()
        WIDTH,
        YEAR
    }
    
    String getFileExtension();
    
    Map<PropertyKey, Object> getProperties();

    Object getProperty(PropertyKey key);
    
    Category getCategory();

    long getSize();
    
    List<RemoteHost> getSources();

    URN getUrn();
    
    public boolean isSpam();
    
    String getFileName();
    
    String getMagnetURL();
}