package org.limewire.ui.swing.search.model;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Provider;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.FunctionList.AdvancedFunction;

/**
 * The default implementation of SearchResultsModel containing the results of
 * a search.  This assembles search results into grouped, filtered, and sorted
 * lists, provides access to details about the search request, and handles 
 * requests to download a search result.
 */
class BasicSearchResultsModel extends AbstractSearchResultsModel {
    private final Log LOG = LogFactory.getLog(getClass());

    /** Descriptor containing search details. */
    private final SearchInfo searchInfo;
    
    /** Search request object. */
    private final Search search;

    /** List of all search results. */
    private final EventList<SearchResult> allSearchResults;

    /** Total number of search results. */
    private final AtomicInteger resultCount = new AtomicInteger();

    /** List of search results grouped by URN. */
    private final FunctionList<List<SearchResult>, VisualSearchResult> groupedUrnResults;

    /** Listener to handle search request events. */
    private SearchListener searchListener;
    

    /**
     * Constructs a BasicSearchResultsModel with the specified search details,
     * search request object, and services.
     */
    public BasicSearchResultsModel(SearchInfo searchInfo, Search search, 
            Provider<PropertiableHeadings> propertiableHeadings,
            DownloadListManager downloadListManager,
            Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler) {
        super(search, downloadListManager, saveLocationExceptionHandler);
        
        this.searchInfo = searchInfo;
        this.search = search;
        
        // Create list of all search results.  Must be thread safe for EventSelectionModel to work properly.
        allSearchResults = GlazedListsFactory.threadSafeList(new BasicEventList<SearchResult>());
        
        // Create list of search results grouped by URN.
        GroupingList<SearchResult> groupingListUrns = GlazedListsFactory.groupingList(
                allSearchResults, new UrnComparator());
        
        // Create list of visual search results where each element represents
        // a single group.
        groupedUrnResults = GlazedListsFactory.functionList(
                groupingListUrns, new SearchResultGrouper(resultCount, propertiableHeadings));
        
        initialize(allSearchResults, groupedUrnResults);        
        
        // Initialize display category and sorted list.
        setSelectedCategory(searchInfo.getSearchCategory());
    }

    /**
     * Installs the specified search listener and starts the search.  The
     * search listener should handle search results by calling the 
     * <code>addSearchResult(SearchResult)</code> method.
     */
    @Override
    public void start(SearchListener searchListener) {
        if (searchListener == null) {
            throw new IllegalArgumentException("Search listener cannot be null");
        }
        
        // Install search listener.
        this.searchListener = searchListener;
        search.addSearchListener(searchListener);
        
        // Start search.
        search.start();
    }
    
    /**
     * Stops the search and removes the current search listener. 
     */
    @Override
    public void dispose() {
        // Stop search.
        search.stop();
        
        // Remove search listener.
        if (searchListener != null) {
            search.removeSearchListener(searchListener);
            searchListener = null;
        }
        
        if (allSearchResults instanceof TransformedList){
            ((TransformedList)allSearchResults).dispose();
        }
        
        super.dispose();
    }
    
    @Override
    public SearchCategory getFilterCategory() {
        return searchInfo.getSearchCategory();
    }
    
    @Override
    public SearchCategory getSearchCategory() {
        return searchInfo.getSearchCategory();
    }
    
    @Override
    public String getSearchQuery() {
        return searchInfo.getSearchQuery();
    }
    
    @Override
    public String getSearchTitle() {
        return searchInfo.getTitle();
    }
    
    @Override
    public int getResultCount() {
        return resultCount.get();
    }

    /**
     * Adds the specified search result to the results list.
     */
    @Override
    public void addSearchResult(SearchResult result) {
        if(result.getUrn() == null) {
            // Some results can be missing a URN, specifically
            // secure results.  For now, we drop these.
            // We should figure out a way to show them later on.
            return;
        }
        
        LOG.debugf("Adding result urn: {0} EDT: {1}", result.getUrn(), SwingUtilities.isEventDispatchThread());
        try {
            allSearchResults.add(result);
        } catch (Throwable th) {
            // Throw wrapper exception with detailed message.
            throw new RuntimeException(createMessageDetail("Problem adding result", result), th);
        }
    }

    /**
     * Removes the specified search result from the results list.
     */
    @Override
    public void removeSearchResult(SearchResult result) {
        allSearchResults.remove(result);
    }

    
    /**
     * Returns a detailed message including the specified prefix and search 
     * result.
     */
    private String createMessageDetail(String prefix, SearchResult result) {
        StringBuilder buf = new StringBuilder(prefix);
        
        buf.append(", searchCategory=").append(searchInfo.getSearchCategory());
        buf.append(", resultCategory=").append(result.getCategory());
        buf.append(", unfilteredSize=").append(getUnfilteredList().size());
        buf.append(", filteredSize=").append(getFilteredList().size());
        buf.append(", EDT=").append(SwingUtilities.isEventDispatchThread());
        
        return buf.toString();
    }

    
    /**
     * A comparator used to group search results by URN.
     */
    private static class UrnComparator implements Comparator<SearchResult> {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
            return o1.getUrn().compareTo(o2.getUrn());
        }
    }

    /**
     * A GlazedList function used to transform a group of search results into
     * a single VisualSearchResult.
     */
    private static class SearchResultGrouper implements
            AdvancedFunction<List<SearchResult>, VisualSearchResult> {
        private final AtomicInteger resultCount;
        private final Provider<PropertiableHeadings> propertiableHeadings;

        public SearchResultGrouper(AtomicInteger resultCount, Provider<PropertiableHeadings> propertiableHeadings) {
            this.resultCount = resultCount;
            this.propertiableHeadings = propertiableHeadings;
        }

        @Override
        public void dispose(List<SearchResult> sourceValue, VisualSearchResult transformedValue) {
            resultCount.addAndGet(-transformedValue.getSources().size());
        }

        @Override
        public VisualSearchResult evaluate(List<SearchResult> sourceValue) {
            VisualSearchResult adapter = new SearchResultAdapter(sourceValue, propertiableHeadings);

            resultCount.addAndGet(adapter.getSources().size());
            return adapter;
        }

        @Override
        public VisualSearchResult reevaluate(List<SearchResult> sourceValue,
                VisualSearchResult transformedValue) {
            resultCount.addAndGet(-transformedValue.getSources().size());
            ((SearchResultAdapter) transformedValue).update();
            resultCount.addAndGet(transformedValue.getSources().size());
            return transformedValue;
        }
    }
    
 }
