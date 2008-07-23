package org.limewire.ui.swing.search;

import javax.swing.SwingUtilities;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.search.model.BasicSearchResultsModel;

import com.google.inject.Inject;

class SearchHandlerImpl implements SearchHandler {
    
    private final Navigator navigator;
    private final SearchFactory searchFactory;
    private final SearchResultsPanelFactory panelFactory;
    
    @Inject
    SearchHandlerImpl(Navigator navigator, SearchFactory searchFactory,
            SearchResultsPanelFactory panelFactory) {
        this.navigator = navigator;
        this.searchFactory = searchFactory;
        this.panelFactory = panelFactory;
    }

    @Override
    public void doSearch(final SearchInfo info) {        
        Search search = searchFactory.createSearch(new SearchDetails() {
            @Override
            public SearchCategory getSearchCategory() {
                return info.getSearchCategory();
            }
            
            @Override
            public String getSearchQuery() {
                return info.getQuery();
            }
        });
        
        String panelTitle = info.getTitle();
        final BasicSearchResultsModel model = new BasicSearchResultsModel();
        SearchResultsPanel searchPanel = panelFactory.createSearchResultsPanel(info, model.getVisualSearchResults(), search);
        NavItem item = navigator.addNavigablePanel(NavCategory.SEARCH, panelTitle, searchPanel, true);
        item.select();
        
        search.start(new SearchListener() {
            @Override
            public void handleSearchResult(final SearchResult searchResult) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        model.addSearchResult(searchResult);
                    }
                });
            }
        });
        
    }

}
