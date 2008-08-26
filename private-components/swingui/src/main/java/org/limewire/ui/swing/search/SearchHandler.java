package org.limewire.ui.swing.search;

import org.limewire.core.api.search.Search;

public interface SearchHandler {

    Search doSearch(SearchInfo info);
}