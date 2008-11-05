/**
 * 
 */
package org.limewire.core.api;

import java.util.Collection;
import java.util.HashSet;

public enum FilePropertyKey {
    TITLE,//for audio this is the track name
    AUTHOR,//for audio files this represents the artists name as well -- TODO this is confusing
    BITRATE, // ex. 128, 160, 192, 224, 320
    COMMENTS,
    COMPANY, //TODO this property is not being set on items
    DATE_CREATED,
    FILE_SIZE, // in bytes
    FILES_IN_ARCHIVE, // # of files in an archive file (zip, tar, ...) //TODO this property is not being set on items
    GENRE,
    HEIGHT,
    LENGTH, // in time (hh:mm:ss)
    NAME,
    OWNER,//TODO this property is not being set on items
    PLATFORM,//TODO this property is not being set on items
    QUALITY,
    RATING,//TODO this property is not being set on items
    SAMPLE_RATE, // ex. 44,100 Hz//TODO this property is not being set on items
    // get size from VisualSearchResult.getSize()
    TRACK_NUMBER,
    ALBUM,
    // get type from VisualSearchResult.getFileExtension()
    WIDTH,
    YEAR;
    
    private final static Collection<FilePropertyKey> indexableKeys = new HashSet<FilePropertyKey>(); 

    static {
        indexableKeys.add(ALBUM);
        indexableKeys.add(TITLE);
        indexableKeys.add(AUTHOR);
        indexableKeys.add(COMMENTS);
        indexableKeys.add(COMPANY);
        indexableKeys.add(GENRE);
        indexableKeys.add(NAME);
        indexableKeys.add(OWNER);
        indexableKeys.add(PLATFORM);
    };
    
    public static Collection<FilePropertyKey> getIndexableKeys() {
        return indexableKeys;
    }
}

