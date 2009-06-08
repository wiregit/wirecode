package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.Search;

public interface BrowseSearch extends Search {

    void addBrowseStatusListener(BrowseStatusListener listener);
    
    void removeBrowseStatusListener(BrowseStatusListener listener);

}
