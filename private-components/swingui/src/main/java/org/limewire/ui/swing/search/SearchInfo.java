package org.limewire.ui.swing.search;

import org.limewire.core.api.search.SearchCategory;

public interface SearchInfo {

    String getTitle();
    String getQuery();
    SearchCategory getSearchCategory();
    
}
