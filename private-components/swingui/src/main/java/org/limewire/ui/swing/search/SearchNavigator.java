package org.limewire.ui.swing.search;

import javax.swing.JComponent;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.browse.BrowseSearch;

/** A central hub for showing a new search in the UI. */
public interface SearchNavigator {
    
    /** Adds a new search whose results will show in the given panel. */
    SearchNavItem addSearch(String title, JComponent searchPanel, Search search);
    
    /** Adds a new browse search whose results will show in the given panel. */
    SearchNavItem addSearch(String title, JComponent searchPanel, BrowseSearch search);

    /** Adds a new advanced search panel */
    SearchNavItem addAdvancedSearch();

}
