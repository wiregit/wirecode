package org.limewire.core.api.library;

import java.io.File;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.friend.feature.features.FileOfferer;
import org.limewire.xmpp.api.client.FileMetaData;

/**
 * A File that is displayed in a library
 */
public interface LocalFileItem extends FileItem {
    /** Returns the file this is based on. */
    File getFile();

    /** Offers this file to a Presence that supports FileOfferFeature. */
    FileMetaData offer(FileOfferer fileOfferer);
    
    /** Returns true if this is shared with Gnutella. */
    boolean isSharedWithGnutella();
    
    /** Returns the number of friends this is shared with. */
    int getFriendShareCount();
    
    /** Determines if this file is sharable. */
    boolean isShareable();
    
    /**True if the file is incomplete**/
    boolean isIncomplete();
    
    /**
     * Sets the property of this file item to a new value.
     */
    void setProperty(FilePropertyKey key, Object value);
}
