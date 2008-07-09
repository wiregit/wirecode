package org.limewire.ui.swing.search;

import javax.swing.SwingUtilities;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.Navigator.NavCategory;

public class SearchHandlerImpl implements SearchHandler {
    
    private final Navigator navigator;
    private final SearchFactory searchFactory;
    
    public SearchHandlerImpl(Navigator navigator, SearchFactory searchFactory) {
        this.navigator = navigator;
        this.searchFactory = searchFactory;
    }

    @Override
    public void doSearch(SearchInfo info) {
        String panelTitle = info.getTitle();
        final SearchResultsPanel searchPanel = new SearchResultsPanel(info);
        NavItem item = navigator.addNavigablePanel(NavCategory.SEARCH, panelTitle, searchPanel, true);
        item.select();
     
        Search search = searchFactory.createSearch(new SearchDetails() {
            
        });
        
        search.start(new SearchListener() {
            @Override
            public void handleSearchResult(final SearchResult searchResult) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        searchPanel.addSearchResult(searchResult);
                    }
                });
            }
        });
        
    }

}
