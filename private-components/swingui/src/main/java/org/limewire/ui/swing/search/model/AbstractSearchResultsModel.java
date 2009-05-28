package org.limewire.ui.swing.search.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.DisposalListener;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Provider;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * initialize() must be called by subclasses in their constructors.
 *
 */
public abstract class AbstractSearchResultsModel implements SearchResultsModel {
    private final Log LOG = LogFactory.getLog(getClass());

    /** Core download manager. */
    private final DownloadListManager downloadListManager;

    /** Save exception handler. */
    private final Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler;

    /** List of all search results. */
    private EventList<SearchResult> allSearchResults;

    /** List of search results grouped by URN. */
    private EventList<VisualSearchResult> groupedSearchResults;

    /** Observable list of grouped search results. */
    private ObservableElementList<VisualSearchResult> observableList;

    /** Filtered list of grouped search results. */
    private FilterList<VisualSearchResult> filteredResultList;

    /** Current list of sorted and filtered results. */
    private SortedList<VisualSearchResult> sortedResultList;

    /** Current list of visible, sorted and filtered results. */
    private FilterList<VisualSearchResult> visibleResultList;

    /** Current selected search category. */
    private SearchCategory selectedCategory;
    
    /** Current sort option. */
    private SortOption sortOption;
    
    private List<DisposalListener> disposalListeners = new ArrayList<DisposalListener>();


    private final Search search;

    /**
     * Constructs a BasicSearchResultsModel with the specified search details,
     * search request object, and services.
     */
    public AbstractSearchResultsModel(Search search,
            DownloadListManager downloadListManager,
            Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler) {
        this.search = search;
        this.downloadListManager = downloadListManager;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
       
    }
    
    /**
     * 
     * This must be called by subclasses in their constructor.
     */
    protected void initialize(EventList<SearchResult> allSearchResults, EventList<VisualSearchResult> groupedSearchResults) {

        this.allSearchResults = allSearchResults;
        this.groupedSearchResults = groupedSearchResults;
        
        // Create observable list that fires an event when results are modified.
        observableList = GlazedListsFactory.observableElementList(groupedSearchResults,
                GlazedLists.beanConnector(VisualSearchResult.class));
        
        // Create filtered list. 
        filteredResultList = GlazedListsFactory.filterList(observableList);
    }

    
    /**
     * Stops the search and removes the current search listener. 
     */
    @Override
    public void dispose() {        
        notifyDisposalListeners();
    }

    @Override
    public final EventList<VisualSearchResult> getGroupedSearchResults() {
        return groupedSearchResults;
    }

    @Override
    public final EventList<VisualSearchResult> getObservableSearchResults() {
        return observableList;
    }

    /**
     * Returns a list of filtered results.
     */
    @Override
    public final EventList<VisualSearchResult> getFilteredSearchResults() {
        return filteredResultList;
    }

    /**
     * Returns a list of sorted and filtered results for the selected search
     * category and sort option.  Only visible results are included in the list.
     */
    @Override
    public final EventList<VisualSearchResult> getSortedSearchResults() {
        return visibleResultList;
    }

    /**
     * Returns the selected search category.
     */
    @Override
    public final SearchCategory getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * Selects the specified search category.  If the selected category is
     * changed, this method updates the sorted list.
     */
    @Override
    public final void setSelectedCategory(SearchCategory selectedCategory) {
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
    public final void setSortOption(SortOption sortOption) {
        this.sortOption = sortOption;
        sortedResultList.setComparator((sortOption != null) ? SortFactory.getSortComparator(sortOption) : null);
    }
    
    /**
     * Sets the MatcherEditor used to filter search results. 
     */
    @Override
    public final void setFilterEditor(MatcherEditor<VisualSearchResult> editor) {
        filteredResultList.setMatcherEditor(editor);
    }


    /**
     * Removes the specified search result from the results list.
     */
    @Override
    public void removeSearchResult(SearchResult result) {
        allSearchResults.remove(result);
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
    
    @Override
    public final EventList<VisualSearchResult> getUnfilteredList() {
        return observableList;
    }
    
    @Override
    public final EventList<VisualSearchResult> getFilteredList() {
        return filteredResultList;
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
    
 

    @Override
    public final void addDisposalListener(DisposalListener listener) {
        disposalListeners.add(listener);
    }

    @Override
    public final void removeDisposalListener(DisposalListener listener) {
        disposalListeners.remove(listener);
    }
    
    private void notifyDisposalListeners(){
        for (DisposalListener listener : disposalListeners){
            listener.objectDisposed(this);
        }
        disposalListeners.clear();
    }
}
