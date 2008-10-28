/**
 * 
 */
package org.limewire.core.api;

public enum FilePropertyKey {
    TITLE,
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
    YEAR;
    
    private static FilePropertyKey[] indexableKeys = {TITLE, AUTHOR, COMMENTS, COMPANY, GENRE, NAME, OWNER, PLATFORM, TRACK_NAME};
    
    public static FilePropertyKey[] getIndexableKeys() {
        return indexableKeys;
    }
}

