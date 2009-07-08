package org.limewire.ui.swing.search.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.DisposalListener;
import org.limewire.ui.swing.filter.FilterDebugger;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.ui.swing.util.DownloadExceptionHandler;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
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
    private static final Log LOG = LogFactory.getLog(BasicSearchResultsModel.class);
    
    /** Filter debugger associated with this model. */
    private final FilterDebugger<VisualSearchResult> filterDebugger;

    /** Descriptor containing search details. */
    private final SearchInfo searchInfo;
    
    /** Search request object. */
    private final Search search;

    /** Core download manager. */
    private final DownloadListManager downloadListManager;

    /** Download exception handler. */
    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;

    /** Total number of search results. */
    private int resultCount;

    /** List of search results grouped by URN. */
    private final EventList<VisualSearchResult> groupedUrnResults;

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
    
    /** Headings to create search results with. */
    private final Provider<PropertiableHeadings> propertiableHeadings;
    
    private final ListQueuer listQueuer = new ListQueuer();

    /**
     * Constructs a BasicSearchResultsModel with the specified search details,
     * search request object, and services.
     */
    public BasicSearchResultsModel(SearchInfo searchInfo, Search search, 
            Provider<PropertiableHeadings> propertiableHeadings,
            DownloadListManager downloadListManager,
            Provider<DownloadExceptionHandler> downloadExceptionHandler) {
        
        this.searchInfo = searchInfo;
        this.search = search;
        this.downloadListManager = downloadListManager;
        this.downloadExceptionHandler = downloadExceptionHandler;
        this.propertiableHeadings = propertiableHeadings;
        
        // Create filter debugger.
        filterDebugger = new FilterDebugger<VisualSearchResult>();
        
        // Underlying list, with no locks -- always accessed on EDT.
        groupedUrnResults = new BasicEventList<VisualSearchResult>(new ReadWriteLock() {
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
        
        groupedUrnResults.dispose();
        
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
        return resultCount;
    }
    
    @Override
    public SearchType getSearchType() {
        return searchInfo.getSearchType();
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
            
        } catch (final DownloadException e) {
            if (e.getErrorCode() == DownloadException.ErrorCode.FILE_ALREADY_DOWNLOADING) {
                DownloadItem downloadItem = downloadListManager.getDownloadItem(vsr.getUrn());
                if (downloadItem != null) {
                    downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                    vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                    if (saveFile != null) {
                        try {
                            // Update save file in DownloadItem.
                            downloadItem.setSaveFile(saveFile, true);
                        } catch (DownloadException ex) {
                            LOG.infof(ex, "Unable to relocate downloading file {0}", ex.getMessage());
                        }
                    }
                }
            } else {
                downloadExceptionHandler.get().handleDownloadException(new DownloadAction() {
                    @Override
                    public void download(File saveFile, boolean overwrite)
                            throws DownloadException {
                        DownloadItem di = downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, overwrite);
                        di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                        vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                    }

                    @Override
                    public void downloadCanceled(DownloadException ignored) {
                        //nothing to do                        
                    }

                }, e, true);
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
    
    private final ResultFinder resultFinder = new ResultFinder();
    private void addResultsInternal(Collection<? extends SearchResult> results) {
        for(SearchResult result : results) {
            URN urn = result.getUrn();
            if(urn != null) {
                int idx = Collections.binarySearch(groupedUrnResults, urn, resultFinder);
                if(idx >= 0) {
//                    System.out.println("Found existing @ " + idx + ", adding new result");
                    SearchResultAdapter vsr = (SearchResultAdapter)groupedUrnResults.get(idx);
                    vsr.addNewSource(result);
                    groupedUrnResults.set(idx, vsr);
                } else {
                    idx = -(idx + 1);
//                    System.out.println("No existing, adding new @ " + idx);
                    SearchResultAdapter vsr = new SearchResultAdapter(result, propertiableHeadings);
                    groupedUrnResults.add(idx, vsr);
                }
                resultCount += result.getSources().size();
            }
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
        private final Object LOCK = new Object();
        private final List<SearchResult> queue = new ArrayList<SearchResult>();
        private final ArrayList<SearchResult> transferQ = new ArrayList<SearchResult>();
        private boolean scheduled = false;
        
        void add(SearchResult result) {
            synchronized(LOCK) {
                queue.add(result);
                schedule();
            }
        }
        
        void addAll(Collection<? extends SearchResult> results) {
            synchronized(LOCK) {
                queue.addAll(results);
                schedule();
            }
        }
        
        void clear() {
            synchronized(LOCK) {
                queue.clear();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        groupedUrnResults.clear();
                    }
                });
            }
        }
        
        private void schedule() {
            if(!scheduled) {
                scheduled = true;
                // purposely SwingUtilities & not SwingUtils so
                // that we force it to the back of the stack
                SwingUtilities.invokeLater(this);
            }
        }
        
        @Override
        public void run() {
            // move to transferQ inside lock
            transferQ.clear();
            synchronized(LOCK) {
                transferQ.addAll(queue);
                queue.clear();
            }
            
            // move to searchResults outside lock,
            // so we don't hold a lock while allSearchResults
            // triggers events.
            addResultsInternal(transferQ);
            transferQ.clear();
            
            synchronized(LOCK) {
                scheduled = false;
                // If the queue wasn't empty, we need to reschedule
                // ourselves, because something got added to the queue
                // without scheduling itself, since we had scheduled=true
                if(!queue.isEmpty()) {
                    schedule();
                }
            }
        }
    }
    
    private static class ResultFinder implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            return ((VisualSearchResult)o1).getUrn().compareTo(((URN)o2));
        }
    }
}
