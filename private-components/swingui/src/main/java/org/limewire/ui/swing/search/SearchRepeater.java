package org.limewire.ui.swing.search;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.SearchResultsModel;

/**
 * Encapsulates logic for refreshing a Search
 */
public class SearchRepeater {
    
    private Search search;
    private SearchResultsModel searchResultsModel;

    public SearchRepeater(Search browseSearch, SearchResultsModel searchResultsModel){
        this.search = browseSearch;
        this.searchResultsModel = searchResultsModel;
    }
    
    public void refresh(){
        searchResultsModel.clear();
        search.repeat();
    }

}
