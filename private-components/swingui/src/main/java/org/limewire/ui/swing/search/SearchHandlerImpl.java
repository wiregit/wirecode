package org.limewire.ui.swing.search;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.api.connection.ConnectionLifeCycleEventType;
import org.limewire.core.api.connection.ConnectionLifeCycleListener;
import org.limewire.core.api.connection.ConnectionManager;
import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

class SearchHandlerImpl implements SearchHandler {
    
    private final SearchFactory searchFactory;
    private final SearchResultsPanelFactory panelFactory;
    private final SearchNavigator searchNavigator;
    private final SearchResultsModelFactory searchResultsModelFactory;
    private final LifeCycleManager lifeCycleManager;
    private final ConnectionManager connectionManager;
    
    @Inject
    SearchHandlerImpl(SearchFactory searchFactory,
            SearchResultsPanelFactory panelFactory,
            SearchNavigator searchNavigator,
            SearchResultsModelFactory searchResultsModelFactory,
            LifeCycleManager lifeCycleManager, 
            ConnectionManager connectionManager) {
        this.searchNavigator = searchNavigator;
        this.searchFactory = searchFactory;
        this.panelFactory = panelFactory;
        this.searchResultsModelFactory = searchResultsModelFactory;
        this.lifeCycleManager = lifeCycleManager;
        this.connectionManager = connectionManager;
    }

    @Override
    public void doSearch(final SearchInfo info) {        
        final SearchCategory searchCategory = info.getSearchCategory();

        final Search search = searchFactory.createSearch(new SearchDetails() {
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
                info, model.getObservableSearchResults(), search);
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
        
        addConnectionWarnings(search, searchPanel);
        
        startSearch(search, searchPanel);
    }

    private void startSearch(final Search search, final SearchResultsPanel searchPanel) {
        //prevent search from starting until lifecycle manager completes loading
        if(lifeCycleManager.isStarted()) {
            search.start();
        } else {
             searchPanel.setLifeCycleComplete(false);
             lifeCycleManager.addListener(new EventListener<LifeCycleEvent>() {
                 final EventListener<LifeCycleEvent> eventListener = this;
                 public void handleEvent(LifeCycleEvent event) {
                     if(event == LifeCycleEvent.STARTED) {
                         SwingUtils.invokeLater(new Runnable() {
                             public void run() {
                                 searchPanel.setLifeCycleComplete(true);
                                 search.start();
                           
                             }
                         });
                         lifeCycleManager.removeListener(eventListener);
                     }
                 }
             });   
        }
    }

    private void addConnectionWarnings(final Search search, final SearchResultsPanel searchPanel) {
        //display search warning message until fully connected to limewire.
        if(!connectionManager.isConnected() || !connectionManager.isFullyConnected()) {
            searchPanel.setFullyConnected(false);
            connectionManager.addEventListener( new ConnectionLifeCycleListener() {
               private final ConnectionLifeCycleListener connectionLifecycleListener = this;
               @Override
                public void handleEvent(ConnectionLifeCycleEventType eventType) {
                   if(eventType == ConnectionLifeCycleEventType.CONNECTED) {
                       if(connectionManager.isFullyConnected()) {
                           SwingUtils.invokeLater(new Runnable() {
                               public void run() {
                                   searchPanel.setFullyConnected(true);
                               }
                           });
                           connectionManager.removeEventListener(connectionLifecycleListener);
                       }
                   }
                } 
            });
            
            //override warning message if a certain number of search results comes in, for now 10, 
            //we assume that while not fully connected, we have a good enough search going
            search.addSearchListener(new SearchListener() {
                private final AtomicInteger numberOfResults = new AtomicInteger(0);
                
                @Override
                public void handleSearchResult(SearchResult searchResult) {
                    if(numberOfResults.addAndGet(1) > 10) {
                        SwingUtils.invokeLater(new Runnable() {
                            public void run() {
                                //while not fully connected, assume the connections we have are enough 
                                //based on the number of results coming in.
                                searchPanel.setFullyConnected(true);
                            }});
                        search.removeSearchListener(this);
                    }
                }

                @Override
                public void handleSponsoredResults(List<SponsoredResult> sponsoredResults) {
                    
                }

                @Override
                public void searchStarted() {
                    
                }

                @Override
                public void searchStopped() {
                    
                }
                
            });
        }
    }
}