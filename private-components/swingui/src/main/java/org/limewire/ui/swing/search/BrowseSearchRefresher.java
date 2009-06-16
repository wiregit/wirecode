package org.limewire.ui.swing.search;

import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.ui.swing.search.model.SearchResultsModel;

/**
 * Encapsulates logic for refreshing a BrowseSearch
 */
public class BrowseSearchRefresher {
    
    private BrowseSearch browseSearch;
    private SearchResultsModel searchResultsModel;

    public BrowseSearchRefresher(BrowseSearch browseSearch, SearchResultsModel searchResultsModel){
        this.browseSearch = browseSearch;
        this.searchResultsModel = searchResultsModel;
    }
    
    public void refresh(){
        searchResultsModel.clear();
        browseSearch.repeat();
    }

}
