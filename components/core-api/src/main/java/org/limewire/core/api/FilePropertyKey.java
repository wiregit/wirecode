/**
 * 
 */
package org.limewire.core.api;

public enum FilePropertyKey {
    TITLE,//for audo files this is the track name -- TODO this is confusing
    AUTHOR,//for audio files this represents the artists name as well -- TODO this is confusing
    BITRATE, // ex. 128, 160, 192, 224, 320
    COMMENTS,
    COMPANY, //TODO this property is not being set on items
    DATE_CREATED,
    FILE_SIZE, // in megabytes
    FILES_IN_ARCHIVE, // # of files in an archive file (zip, tar, ...) //TODO this property is not being set on items
    GENRE,
    HEIGHT,
    LENGTH, // in time (hh:mm:ss)
    NAME,
    OWNER,//TODO this property is not being set on items
    PLATFORM,//TODO this property is not being set on items
    QUALITY,
    RATING,//TODO this property is not being set on items
    RELEVANCE,//TODO this property is not being set on items
    SAMPLE_RATE, // ex. 44,100 Hz//TODO this property is not being set on items
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

