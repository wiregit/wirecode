package org.limewire.ui.swing.search;

public class DefaultSearchInfo implements SearchInfo {

    private final String query;
    private final SearchCategory searchCategory;

    public DefaultSearchInfo(String query, SearchCategory searchCategory) {
        this.query = query;
        this.searchCategory = searchCategory;
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public String getTitle() {
        return query;
    }
    
    @Override
    public SearchCategory getSearchCategory() {
        return searchCategory;
    }

}
