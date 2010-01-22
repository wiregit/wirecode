package org.limewire.core.impl.search;

import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;

/**
 * Mock implementation of SearchDetails for unit tests.
 */
class TestSearchDetails implements SearchDetails {

    @Override
    public Map<FilePropertyKey, String> getAdvancedDetails() {
        return null;
    }

    @Override
    public SearchCategory getSearchCategory() {
        return null;
    }

    @Override
    public String getSearchQuery() {
        return null;
    }

    @Override
    public SearchType getSearchType() {
        return null;
    }

}
