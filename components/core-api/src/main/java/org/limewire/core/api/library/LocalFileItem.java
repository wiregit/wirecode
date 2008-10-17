package org.limewire.core.api.library;

import java.io.File;

import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.FileMetaData;

/**
 * A File that is displayed in a library
 */
public interface LocalFileItem extends FileItem {
    /** Returns the file this is based on. */
    File getFile();

    /** Offers this file to a LimePresence. */
    FileMetaData offer(LimePresence limePresence);
    
    /** Returns true if this is shared with Gnutella. */
    boolean isSharedWithGnutella();
    
    /** Returns the number of friends this is shared with. */
    int getFriendShareCount();
    
    /** Determines if this file is sharable. */
    boolean isShareable();
    
    /**True if the file is incomplete**/
    boolean isIncomplete();
}
