package org.limewire.ui.swing.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The implementation of SearchHandler used to initiate regular text searches.
 * Calling the <code>doSearch(SearchInfo)</code> method will create a new  
 * search results tab in the UI, and start the search operation.
 */
@Singleton
class TextSearchHandlerImpl implements SearchHandler {
    
    private final SearchFactory searchFactory;
    private final SearchResultsPanelFactory panelFactory;
    private final SearchNavigator searchNavigator;
    private final SearchResultsModelFactory searchResultsModelFactory;
    private final LifeCycleManager lifeCycleManager;
    private final GnutellaConnectionManager connectionManager;
    
    /**
     * Constructs a TextSearchHandlerImpl with the specified services and
     * factories.
     */
    @Inject
    TextSearchHandlerImpl(SearchFactory searchFactory,
            SearchResultsPanelFactory panelFactory,
            SearchNavigator searchNavigator,
            SearchResultsModelFactory searchResultsModelFactory,
            LifeCycleManager lifeCycleManager, 
            GnutellaConnectionManager connectionManager) {
        this.searchNavigator = searchNavigator;
        this.searchFactory = searchFactory;
        this.panelFactory = panelFactory;
        this.searchResultsModelFactory = searchResultsModelFactory;
        this.lifeCycleManager = lifeCycleManager;
        this.connectionManager = connectionManager;
    }

    /**
     * Performs a search operation using the specified SearchInfo object.  
     * The method always returns true.
     */
    @Override
    public boolean doSearch(final SearchInfo info) {
        // Create search request.
        final Search search = searchFactory.createSearch(info);
        
        String panelTitle = info.getTitle();
        
        // Create search results data model and display panel.
        final SearchResultsModel model = searchResultsModelFactory.createSearchResultsModel();
        final SearchResultsPanel searchPanel = panelFactory.createSearchResultsPanel(
                info, model.getObservableSearchResults(), search);
        
        // Add search results display to the UI, and select its navigation item.
        final SearchNavItem item = searchNavigator.addSearch(panelTitle, searchPanel, search);
        item.select();
        
        // Add listener to forward search results to data model.
        search.addSearchListener(new SearchListener() {
            @Override
            public void handleSearchResult(Search search, final SearchResult searchResult) {
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
            public void searchStarted(Search search) {
            }

            @Override
            public void searchStopped(Search search) {
            }

            @Override
            public void handleSponsoredResults(Search search, final List<SponsoredResult> sponsoredResults) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        searchPanel.addSponsoredResults(sponsoredResults);
                    }
                });
            }
        });

        // Add listeners for connection events.
        addConnectionWarnings(search, searchPanel, item);
        
        // Start search operation.
        startSearch(search, searchPanel, item);
        return true;
    }

    /**
     * Initiates a search using the specified Search request, display panel,
     * and navigation item.
     */
    private void startSearch(final Search search, final SearchResultsPanel searchPanel, NavItem navItem) {
        //prevent search from starting until lifecycle manager completes loading
        if(lifeCycleManager.isStarted()) {
            search.start();
        } else {
             searchPanel.setLifeCycleComplete(false);
             final EventListener<LifeCycleEvent> listener = new EventListener<LifeCycleEvent>() {
                 @SwingEDTEvent
                 public void handleEvent(LifeCycleEvent event) {
                     if(event == LifeCycleEvent.STARTED) {
                         searchPanel.setLifeCycleComplete(true);
                         search.start();
                         lifeCycleManager.removeListener(this);
                     }
                 }
             };
             lifeCycleManager.addListener(listener);
             navItem.addNavItemListener(new NavItemListener() {                 
                 public void itemRemoved() {
                     lifeCycleManager.removeListener(listener);
                 }
                 
                 public void itemSelected(boolean selected) {}
             });
        }
    }
    
    /**
     * Notifies the specified SearchResultsPanel if the specified
     * ConnectionStrength indicates a full connection.  Return true if fully
     * connected, false otherwise.
     */
    private boolean setConnectionStrength(ConnectionStrength type, SearchResultsPanel searchPanel) {
        switch(type) {
        case TURBO:
        case FULL:
            searchPanel.setFullyConnected(true);
            return true;
        }
        
        return false;
    }
    
    /**
     * Removes the specified listeners from the Search request and connection 
     * manager.
     */
    private void removeListeners(Search search, AtomicReference<SearchListener> searchListenerRef, AtomicReference<PropertyChangeListener> connectionListenerRef) {
        SearchListener searchListener = searchListenerRef.get();
        if(searchListener != null) {
            search.removeSearchListener(searchListener);
            searchListenerRef.set(null);
        }
        
        PropertyChangeListener connectionListener = connectionListenerRef.get();
        if(connectionListener != null) {
            connectionManager.removePropertyChangeListener(connectionListener);
            connectionListenerRef.set(null);
        }
    }

    /**
     * Adds listeners to the Search request and connection manager to update 
     * the UI based on connection events.
     */
    private void addConnectionWarnings(final Search search, final SearchResultsPanel searchPanel, NavItem navItem) {
        final AtomicReference<SearchListener> searchListenerRef = new AtomicReference<SearchListener>();
        final AtomicReference<PropertyChangeListener> connectionListenerRef = new AtomicReference<PropertyChangeListener>();
        
        connectionListenerRef.set(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("strength")) {
                    if(setConnectionStrength((ConnectionStrength)evt.getNewValue(), searchPanel)) {
                        removeListeners(search, searchListenerRef, connectionListenerRef);
                    }
                }
            }
        });  

        //override warning message if a certain number of search results comes in, for now 10, 
        //we assume that while not fully connected, we have a good enough search going
        searchListenerRef.set(new SearchListener() {
            private final AtomicInteger numberOfResults = new AtomicInteger(0);
            
            @Override
            public void handleSearchResult(Search search, SearchResult searchResult) {
                if(numberOfResults.addAndGet(1) > 10) {
                    SwingUtils.invokeLater(new Runnable() {
                        public void run() {
                            // while not fully connected, assume the
                            // connections we have are enough
                            // based on the number of results coming in.
                            searchPanel.setFullyConnected(true);
                        }
                    });
                    removeListeners(search, searchListenerRef, connectionListenerRef);
                }
            }

            @Override public void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults) {}
            @Override public void searchStarted(Search search) {}
            @Override public void searchStopped(Search search) {}                
        });
        
        searchPanel.setFullyConnected(false);
        connectionManager.addPropertyChangeListener(connectionListenerRef.get());        
        if(setConnectionStrength(connectionManager.getConnectionStrength(), searchPanel)) {
            removeListeners(search, searchListenerRef, connectionListenerRef);
        } else {
            search.addSearchListener(searchListenerRef.get());
            navItem.addNavItemListener(new NavItemListener() {
                @Override
                public void itemRemoved() {
                    removeListeners(search, searchListenerRef, connectionListenerRef);
                }
                @Override public void itemSelected(boolean selected) {}
            });
        }
        
    }
}