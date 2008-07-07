package org.limewire.ui.swing.search;

public class DefaultSearchInfo implements SearchInfo {

    private final String query;

    public DefaultSearchInfo(String query) {
        this.query = query;
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public String getTitle() {
        return query;
    }

}
