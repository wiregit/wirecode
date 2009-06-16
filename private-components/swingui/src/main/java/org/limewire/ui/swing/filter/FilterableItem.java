package org.limewire.ui.swing.filter;

import java.util.Collection;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.PropertiableFile;

/**
 * Defines an item that can be filtered.  Known implementations include
 * {@link org.limewire.ui.swing.search.model.VisualSearchResult VisualSearchResult}.
 */
public interface FilterableItem extends PropertiableFile {

    /**
     * Returns an indicator that determines if the item is from an anonymous
     * source.
     */
    boolean isAnonymous();
    
    /**
     * Returns the file extension for the item.
     */
    String getFileExtension();
    
    /**
     * Returns a Collection of friends that are sources for the item.
     */
    Collection<Friend> getFriends();
    
    /**
     * Returns the size of the item in bytes.
     */
    long getSize();
    
    /**
     * Returns an indicator that determines if the result is spam.
     */
    boolean isSpam();
    
    /**
     * Returns a map containing FilePropertyKey objects and their associated
     * values. 
     */
    Map<FilePropertyKey, Object> getProperties();
    
}
