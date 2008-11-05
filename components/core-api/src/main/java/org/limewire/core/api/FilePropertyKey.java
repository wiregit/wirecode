/**
 * 
 */
package org.limewire.core.api;

import java.util.Collection;
import java.util.HashSet;

public enum FilePropertyKey {
    TITLE,//for audio this is the track name
    AUTHOR,//for audio files this represents the artists name
    BITRATE, // ex. 128, 160, 192, 224, 320
    COMMENTS,
    COMPANY,//for video files this is the studio, for applications the publisher
    DATE_CREATED,
    FILE_SIZE, // in bytes
    FILES_IN_ARCHIVE, // # of files in an archive file (zip, tar, ...) //TODO this property is not being set on items
    GENRE,
    HEIGHT,
    LENGTH, // in seconds
    NAME,
    PLATFORM,
    QUALITY,
    RATING,
    TRACK_NUMBER,
    ALBUM,
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
        indexableKeys.add(PLATFORM);
    };
    
    public static Collection<FilePropertyKey> getIndexableKeys() {
        return indexableKeys;
    }
}

