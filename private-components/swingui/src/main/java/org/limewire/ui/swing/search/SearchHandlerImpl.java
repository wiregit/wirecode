package org.limewire.ui.swing.search;

import java.util.List;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

class SearchHandlerImpl implements SearchHandler {
    
    private final SearchFactory searchFactory;
    private final SearchResultsPanelFactory panelFactory;
    private final SearchNavigator searchNavigator;
    private final SearchResultsModelFactory searchResultsModelFactory;
    
    @Inject
    SearchHandlerImpl(SearchFactory searchFactory,
            SearchResultsPanelFactory panelFactory,
            SearchNavigator searchNavigator,
            SearchResultsModelFactory searchResultsModelFactory) {
        this.searchNavigator = searchNavigator;
        this.searchFactory = searchFactory;
        this.panelFactory = panelFactory;
        this.searchResultsModelFactory = searchResultsModelFactory;
    }

    @Override
    public void doSearch(final SearchInfo info) {        
        final SearchCategory searchCategory = info.getSearchCategory();

        Search search = searchFactory.createSearch(new SearchDetails() {
            @Override
            public SearchCategory getSearchCategory() {
                return searchCategory;
            }
            
            @Override
            public String getSearchQuery() {
                return info.getQuery();
            }
        });
        
        String panelTitle = info.getTitle();
        final SearchResultsModel model = searchResultsModelFactory.createSearchResultsModel();
        final SearchResultsPanel searchPanel =
            panelFactory.createSearchResultsPanel(
                info, model.getVisualSearchResults(), search);
        final SearchNavItem item =
            searchNavigator.addSearch(panelTitle, searchPanel, search);
        item.select();
        
        search.addSearchListener(new SearchListener() {
            @Override
            public void handleSearchResult(final SearchResult searchResult) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // Expect that the core properly filtered the result.
                        // That is, we won't see it if we didn't want to.
                        model.addSearchResult(searchResult);
                        
                        // We can update the source count here because
                        // we never expect things to be removed.
                        // Changes only happen on insertion.
                        // If removes ever happen, we'll need to switch
                        // to adding a ListEventListener to
                        // model.getVisualSearchResults.
                        item.sourceCountUpdated(model.getResultCount());
                    }
                });
            }

            @Override
            public void searchStarted() {
            }

            @Override
            public void searchStopped() {
            }

            @Override
            public void handleSponsoredResults(final List<SponsoredResult> sponsoredResults) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        searchPanel.addSponsoredResults(sponsoredResults);
                    }
                });
            }
        });
        
        search.start();
    }
}