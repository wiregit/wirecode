package org.limewire.ui.swing.search.model;

import java.util.Collection;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.components.DisposalListenerList;
import org.limewire.ui.swing.filter.FilterableSource;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Defines a data model containing the results of a search.
 */
public interface SearchResultsModel extends FilterableSource<VisualSearchResult>, 
                                            DownloadHandler, 
                                            DisposalListenerList {

    /**
     * Installs the specified search listener and starts the search.  The
     * search listener should handle search results by calling the 
     * <code>addSearchResult(SearchResult)</code> method. 
     */
    void start(SearchListener searchListener);
    
    /**
     * Returns the search category.
     */
    SearchCategory getSearchCategory();
    
    /**
     * Returns the query string for the search.
     */
    String getSearchQuery();
    
    /**
     * Returns the title string for the search.
     */
    String getSearchTitle();
    
    /**
     * Returns the total number of results in the search.
     */
    int getResultCount();

    /**
     * Returns a list of filtered results in the search.
     */
    EventList<VisualSearchResult> getFilteredSearchResults();

    /**
     * Returns a list of sorted and filtered results for the selected search
     * category and sort option.
     */
    EventList<VisualSearchResult> getSortedSearchResults();

    /**
     * Returns the selected search category.
     */
    SearchCategory getSelectedCategory();

    /**
     * Selects the specified search category.
     */
    void setSelectedCategory(SearchCategory searchCategory);
    
    /**
     * Sets the sort option.
     */
    void setSortOption(SortOption sortOption);

    /**
     * Sets the MatcherEditor used to filter search results. 
     */
    void setFilterEditor(MatcherEditor<VisualSearchResult> editor);
    
    /**
     * Adds the specified search result to the results list.
     */
    void addSearchResult(SearchResult result);
    
    /**
     * Adds all the search results to the results list.
     */
    void addSearchResults(Collection<? extends SearchResult> result);
    
    /**
     * Adds the specified store result to the results list.
     */
    void addStoreResult(StoreResult storeResult);
    
    /**
     * Removes all results from the model
     */
    void clear();

    /**
     * @return The type of the search
     */
    SearchType getSearchType();
    
    /**
     * Returns the style for store results.
     */
    StoreStyle getStoreStyle();
    
    /**
     * Sets the store style to the specified value.
     */
    void setStoreStyle(StoreStyle storeStyle);
    
    /**
     * Adds the specified listener to the list that is notified when the 
     * model data is updated.
     */
    void addModelListener(ModelListener listener);
    
    /**
     * Removes the specified listener from the list that is notified when the 
     * model data is updated.
     */
    void removeModelListener(ModelListener listener);
    
    /**
     * Defines a listener that is notified when model data is updated.
     */
    public interface ModelListener {
        
        /** Invoked when the specified store result is updated. */
        void storeResultUpdated(VisualStoreResult vsr);
        
        /** Invoked when the style for store results is updated. */
        void storeStyleUpdated(StoreStyle storeStyle);
    }
}
