package org.limewire.ui.swing.search;

import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.browse.BrowseSearch;

public class BrowserRefresher {
    
    private BrowseSearch browseSearch;
    private SearchResultsModel searchResultsModel;

    public BrowserRefresher(BrowseSearch browseSearch, SearchResultsModel searchResultsModel){
        this.browseSearch = browseSearch;
        this.searchResultsModel = searchResultsModel;
    }
    
    public void refresh(){
        browseSearch.stop();
        searchResultsModel.clear();
        browseSearch.repeat();
    }

}
