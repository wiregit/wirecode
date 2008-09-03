package org.limewire.ui.swing.search;

import java.util.List;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.model.BasicSearchResultsModel;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import org.limewire.core.api.search.ResultType;

class SearchHandlerImpl implements SearchHandler {
    
    private final SearchFactory searchFactory;
    private final SearchResultsPanelFactory panelFactory;
    private final SearchNavigator searchNavigator;
    
    @Inject
    SearchHandlerImpl(SearchFactory searchFactory,
            SearchResultsPanelFactory panelFactory,
            SearchNavigator searchNavigator) {
        this.searchNavigator = searchNavigator;
        this.searchFactory = searchFactory;
        this.panelFactory = panelFactory;
    }

    @Override
    public Search doSearch(final SearchInfo info) {        
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
        final BasicSearchResultsModel model = new BasicSearchResultsModel();
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
                        ResultType resultType = searchResult.getResultType();

                        // If the result is the type we are looking for ...
                        if (searchCategory == SearchCategory.ALL
                            || resultType.name().equals(searchCategory.name())) {
                            model.addSearchResult(searchResult);
                            // We can update the source count here because
                            // we never expect things to be removed.
                            // Changes only happen on insertion.
                            // If removes ever happen, we'll need to switch
                            // to adding a ListEventListener to
                            // model.getVisualSearchResults.
                            item.sourceCountUpdated(model.getResultCount());
                        }
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

        return search;
    }
}