package org.limewire.ui.swing.search.model.browse;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.BrowseStatusListener;

/**
 * Handles browse host as a Search.
 */
public interface BrowseSearch extends Search {

    void addBrowseStatusListener(BrowseStatusListener listener);
    
    void removeBrowseStatusListener(BrowseStatusListener listener);

}
