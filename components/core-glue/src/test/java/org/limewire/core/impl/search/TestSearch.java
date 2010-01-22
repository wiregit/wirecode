package org.limewire.core.impl.search;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;

/**
 * Mock implementation of Search for unit tests.
 */
class TestSearch implements Search {

    @Override
    public void addSearchListener(SearchListener searchListener) {
    }

    @Override
    public SearchCategory getCategory() {
        return null;
    }

    @Override
    public void removeSearchListener(SearchListener searchListener) {
    }

    @Override
    public void repeat() {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

}
