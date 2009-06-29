package org.limewire.ui.swing.search.model;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.DisposalListener;
import org.limewire.ui.swing.filter.FilterDebugger;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.FunctionList.AdvancedFunction;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.util.concurrent.Lock;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

import com.google.inject.Provider;

/**
 * The default implementation of SearchResultsModel containing the results of
 * a search.  This assembles search results into grouped, filtered, and sorted
 * lists, provides access to details about the search request, and handles 
 * requests to download a search result.
 */
class BasicSearchResultsModel implements SearchResultsModel {
    private final Log LOG = LogFactory.getLog(getClass());
    
    /** Filter debugger associated with this model. */
    private final FilterDebugger<VisualSearchResult> filterDebugger;

    /** Descriptor containing search details. */
    private final SearchInfo searchInfo;
    
    /** Search request object. */
    private final Search search;

    /** Core download manager. */
    private final DownloadListManager downloadListManager;

    /** Save exception handler. */
    private final Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler;

    /** List of all search results. */
    private final EventList<SearchResult> allSearchResults;

    /** Total number of search results. */
    private final AtomicInteger resultCount = new AtomicInteger();

    /** List of search results grouped by URN. */
    private final FunctionList<List<SearchResult>, VisualSearchResult> groupedUrnResults;

    /** Observable list of grouped search results. */
    private final ObservableElementList<VisualSearchResult> observableList;

    /** Filtered list of grouped search results. */
    private final FilterList<VisualSearchResult> filteredResultList;

    /** Listener to handle search request events. */
    private SearchListener searchListener;

    /** Current list of sorted and filtered results. */
    private SortedList<VisualSearchResult> sortedResultList;

    /** Current list of visible, sorted and filtered results. */
    private FilterList<VisualSearchResult> visibleResultList;

    /** Current selected search category. */
    private SearchCategory selectedCategory;
    
    /** Current sort option. */
    private SortOption sortOption;
    
    private List<DisposalListener> disposalListeners = new ArrayList<DisposalListener>();
    
    private final ListQueuer listQueuer = new ListQueuer();

    /**
     * Constructs a BasicSearchResultsModel with the specified search details,
     * search request object, and services.
     */
    public BasicSearchResultsModel(SearchInfo searchInfo, Search search, 
            Provider<PropertiableHeadings> propertiableHeadings,
            DownloadListManager downloadListManager,
            Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler) {
        
        this.searchInfo = searchInfo;
        this.search = search;
        this.downloadListManager = downloadListManager;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        
        // Create filter debugger.
        filterDebugger = new FilterDebugger<VisualSearchResult>();
        
        // Underlying list, with no locks -- always accessed on EDT.
        allSearchResults = new BasicEventList<SearchResult>(new ReadWriteLock() {
            private Lock noopLock = new Lock() {
                @Override public void lock() {}
                @Override public boolean tryLock() { return true; }
                @Override public void unlock() {}
            };
            
            @Override
            public Lock readLock() {
                return noopLock;
            }
            
            @Override
            public Lock writeLock() {
                return noopLock;
            }
        });
        
        // Create list of search results grouped by URN.
        GroupingList<SearchResult> groupingListUrns = GlazedListsFactory.groupingList(
                allSearchResults, new UrnComparator());
        
        // Create list of visual search results where each element represents
        // a single group.
        groupedUrnResults = GlazedListsFactory.functionList(
                groupingListUrns, new SearchResultGrouper(resultCount, propertiableHeadings));
        
        // Create observable list that fires an event when results are modified.
        observableList = GlazedListsFactory.observableElementList(groupedUrnResults,
                GlazedLists.beanConnector(VisualSearchResult.class));
        
        // Create filtered list. 
        filteredResultList = GlazedListsFactory.filterList(observableList);
        
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
        notifyDisposalListeners();
    }
    
    @Override
    public SearchCategory getFilterCategory() {
        return searchInfo.getSearchCategory();
    }
    
    @Override
    public FilterDebugger<VisualSearchResult> getFilterDebugger() {
        return filterDebugger;
    }
    
    @Override
    public EventList<VisualSearchResult> getUnfilteredList() {
        return observableList;
    }
    
    @Override
    public EventList<VisualSearchResult> getFilteredList() {
        return filteredResultList;
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

    @Override
    public EventList<VisualSearchResult> getGroupedSearchResults() {
        return groupedUrnResults;
    }

    @Override
    public EventList<VisualSearchResult> getObservableSearchResults() {
        return observableList;
    }

    /**
     * Returns a list of filtered results.
     */
    @Override
    public EventList<VisualSearchResult> getFilteredSearchResults() {
        return filteredResultList;
    }

    /**
     * Returns a list of sorted and filtered results for the selected search
     * category and sort option.  Only visible results are included in the list.
     */
    @Override
    public EventList<VisualSearchResult> getSortedSearchResults() {
        return visibleResultList;
    }

    /**
     * Returns the selected search category.
     */
    @Override
    public SearchCategory getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * Selects the specified search category.  If the selected category is
     * changed, this method updates the sorted list.
     */
    @Override
    public void setSelectedCategory(SearchCategory selectedCategory) {
        if (this.selectedCategory != selectedCategory) {
            this.selectedCategory = selectedCategory;
            updateSortedList();
        }
    }
    
    /**
     * Updates sorted list of visible results.  This method is called when the
     * selected category is changed.
     */
    private void updateSortedList() {
        // Create visible and sorted lists if necessary.
        if (visibleResultList == null) {
            sortedResultList = GlazedListsFactory.sortedList(filteredResultList, null);
            visibleResultList = GlazedListsFactory.filterList(sortedResultList, new VisibleMatcher());
            sortedResultList.setComparator((sortOption != null) ? SortFactory.getSortComparator(sortOption) : null);
        }
    }
    
    /**
     * Sets the sort option.  This method updates the sorted list by changing 
     * the sort comparator.
     */
    @Override
    public void setSortOption(SortOption sortOption) {
        this.sortOption = sortOption;
        sortedResultList.setComparator((sortOption != null) ? SortFactory.getSortComparator(sortOption) : null);
    }
    
    /**
     * Sets the MatcherEditor used to filter search results. 
     */
    @Override
    public void setFilterEditor(MatcherEditor<VisualSearchResult> editor) {
        filteredResultList.setMatcherEditor(editor);
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
        
//        LOG.debugf("Adding result urn: {0} EDT: {1}", result.getUrn(), SwingUtilities.isEventDispatchThread());
        try {
            listQueuer.add(result);
        } catch (Throwable th) {
            // Throw wrapper exception with detailed message.
            throw new RuntimeException(createMessageDetail("Problem adding result", result), th);
        }
    }
    
    @Override
    public void addSearchResults(Collection<? extends SearchResult> results) {
        boolean ok = true;
        // make certain there's nothing w/o a URN in here
        for(SearchResult result : results) {
            if(result.getUrn() == null) {
                // crap, we need to really work on this list & remove bad items.
                ok = false;
                break;
            }
        }
        
        // filter out any items w/o a URN
        if(!ok) {
            ArrayList<SearchResult> cleanup = new ArrayList<SearchResult>(results);
            for(int i = cleanup.size() - 1; i >= 0; i--) {
                if(cleanup.get(i).getUrn() == null) {
                    cleanup.remove(i);
                }
            }
            results = cleanup;
        }        
        
        listQueuer.addAll(results);
    }
    
    /**
     * Removes all results from the model
     */
    public void clear(){
        listQueuer.clear();
    }
    
    /**
     * Initiates a download of the specified visual search result.
     */
    @Override
    public void download(VisualSearchResult vsr) {
        download(vsr, null);
    }
    
    /**
     * Initiates a download of the specified visual search result to the
     * specified save file.
     */
    @Override
    public void download(final VisualSearchResult vsr, File saveFile) {
        try {
            // Add download to manager.  If save file is specified, then set
            // overwrite to true because the user has already confirmed it.
            DownloadItem di = (saveFile == null) ?
                    downloadListManager.addDownload(search, vsr.getCoreSearchResults()) :
                    downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, true);
            
            // Add listener, and initialize download state.
            di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
            vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
            
        } catch (final SaveLocationException sle) {
            if (sle.getErrorCode() == SaveLocationException.LocationCode.FILE_ALREADY_DOWNLOADING) {
                DownloadItem downloadItem = downloadListManager.getDownloadItem(vsr.getUrn());
                if (downloadItem != null) {
                    downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                    vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                    if (saveFile != null) {
                        try {
                            // Update save file in DownloadItem.
                            downloadItem.setSaveFile(saveFile, true);
                        } catch (SaveLocationException ex) {
                            LOG.infof(ex, "Unable to relocate downloading file {0}", ex.getMessage());
                        }
                    }
                }
            } else {
                saveLocationExceptionHandler.get().handleSaveLocationException(new DownloadAction() {
                    @Override
                    public void download(File saveFile, boolean overwrite)
                            throws SaveLocationException {
                        DownloadItem di = downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, overwrite);
                        di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                        vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                    }

                    @Override
                    public void downloadCanceled(SaveLocationException sle) {
                        //nothing to do                        
                    }

                }, sle, true);
            }
        }
    }
    
    /**
     * Returns a detailed message including the specified prefix and search 
     * result.
     */
    private String createMessageDetail(String prefix, SearchResult result) {
        StringBuilder buf = new StringBuilder(prefix);
        
        buf.append(", searchCategory=").append(searchInfo.getSearchCategory());
        buf.append(", resultCategory=").append(result.getCategory());
        buf.append(", spam=").append(result.isSpam());
        buf.append(", unfilteredSize=").append(getUnfilteredList().size());
        buf.append(", filteredSize=").append(getFilteredList().size());
        buf.append(", EDT=").append(SwingUtilities.isEventDispatchThread());
        buf.append(", filters=").append(filterDebugger.getFilterString());
        
        return buf.toString();
    }

    /**
     * A matcher used to filter visible search results. 
     */
    private static class VisibleMatcher implements Matcher<VisualSearchResult> {
        @Override
        public boolean matches(VisualSearchResult item) {
            return item.isVisible();
        }
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

    @Override
    public void addDisposalListener(DisposalListener listener) {
        disposalListeners.add(listener);
    }

    @Override
    public void removeDisposalListener(DisposalListener listener) {
        disposalListeners.remove(listener);
    }
    
    private void notifyDisposalListeners(){
        for (DisposalListener listener : disposalListeners){
            listener.objectDisposed(this);
        }
    }
    
    private class ListQueuer implements Runnable {
        private volatile boolean scheduled = false;
        private final BlockingQueue<SearchResult> queue = new ArrayBlockingQueue<SearchResult>(50);
        private final List<SearchResult> transferQ = new ArrayList<SearchResult>(50);
        
        void add(SearchResult result) {
            assert !EventQueue.isDispatchThread();
            
            try {
                queue.put(result);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            schedule();
        }
        
        void addAll(Collection<? extends SearchResult> results) {
            assert !EventQueue.isDispatchThread();
            
            for(SearchResult result : results) {
                add(result);
            }
        }
        
        void clear() {
            assert EventQueue.isDispatchThread();
            queue.clear();
            allSearchResults.clear();
        }
        
        private void schedule() {
            if(!scheduled) {
                scheduled = true;
                SwingUtilities.invokeLater(this);
            }
        }
        
        @Override
        public void run() {
            queue.drainTo(transferQ);
            allSearchResults.addAll(transferQ);
            transferQ.clear();
            
            // Something may have gotten added while we were in this method.
            // If that's the case, then just reschedule the run for later.
            // (to avoid starving the event thread of processing other events)
            if(!queue.isEmpty()) {
                // don't use schedule -- just force an invokeLater,
                // schedule is already true.
                SwingUtilities.invokeLater(this);
            } else {
                scheduled = false;
                // if the Q isn't empty after we set scheduled to false,
                // we must reschedule, because it's possible another
                // thread added an item but didn't set schedule to true,
                // because it was already true when that thread had
                // priority.
                if(!queue.isEmpty()) {
                    schedule();
                }
            }
        }
    }
}
