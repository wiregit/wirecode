package org.limewire.ui.swing.search.model;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Implements a factory for creating the search results data model.
 */
public class SearchResultsModelFactory {
    
    private final SpamManager spamManager;

    private final SimilarResultsDetectorFactory similarResultsDetectorFactory;

    private final LibraryManager libraryManager;

    private final DownloadListManager downloadListManager;

    private final Provider<PropertiableHeadings> propertiableHeadings;

    private final Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler;

    private final RemoteLibraryManager remoteLibraryManager;

	private final ListenerSupport<FriendEvent> availListeners;

    /**
     * Constructs a SearchResultsModelFactory with the specified factories,
     * managers, and property values.
     */
    @Inject
    public SearchResultsModelFactory(SimilarResultsDetectorFactory similarResultsDetectorFactory,
            SpamManager spamManager, LibraryManager libraryManager,
            DownloadListManager downloadListManager, Provider<PropertiableHeadings> propertiableHeadings,
            Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler, 
            RemoteLibraryManager remoteLibraryManager, 
            @Named("available")ListenerSupport<FriendEvent> availListeners) {
        this.similarResultsDetectorFactory = similarResultsDetectorFactory;
        this.spamManager = spamManager;
        this.libraryManager = libraryManager;
        this.downloadListManager = downloadListManager;
        this.propertiableHeadings = propertiableHeadings;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.remoteLibraryManager = remoteLibraryManager;
        this.availListeners = availListeners;
    }

    /**
     * Creates a new instance of SearchResultsModel.
     */
    public SearchResultsModel createSearchResultsModel(SearchInfo searchInfo, Search search) {
        // Create search result model.
        SearchResultsModel searchResultsModel = new BasicSearchResultsModel(
                searchInfo, search, propertiableHeadings, downloadListManager, 
                saveLocationExceptionHandler);

        setUpModel(searchResultsModel);
        return searchResultsModel;
    }
    
    /**
     * Creates a new instance of SearchResultsModel for browsing a host.
     */
    public BrowseSearchResultsModel createSearchResultsModel(FriendPresence presence) {
        BrowseSearchResultsModel searchResultsModel = new BrowseSearchResultsModel(presence, remoteLibraryManager,
            propertiableHeadings, downloadListManager, saveLocationExceptionHandler, availListeners);

        setUpModel(searchResultsModel);
        return searchResultsModel;
        
    }
    
    private void setUpModel(SearchResultsModel searchResultsModel ){        

        // Get list of visual search results.
        EventList<VisualSearchResult> visualSearchResults = searchResultsModel.getGroupedSearchResults();
        
        // Create detector to find similar results.
        SimilarResultsDetector similarResultsDetector = similarResultsDetectorFactory
                .newSimilarResultsDetector();

        // Add list listener for results already downloaded or being downloaded. 
        // AlreadyDownloaded listener needs to be added to the list before the
        // grouping listener because the grouping listener uses values set by 
        // the AlreadyDownloaded listener.
        AlreadyDownloadedListEventListener alreadyDownloadedListEventListener = 
                new AlreadyDownloadedListEventListener(libraryManager, downloadListManager);
        visualSearchResults.addListEventListener(alreadyDownloadedListEventListener);

        // Add list listener to group similar results.
        if (SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue()) {
            GroupingListEventListener groupingListEventListener = 
                new GroupingListEventListener(similarResultsDetector);
            visualSearchResults.addListEventListener(groupingListEventListener);
        }

        // Add list listener to handle spam results.
        SpamListEventListener spamListEventListener = new SpamListEventListener(
                spamManager, similarResultsDetector);
        visualSearchResults.addListEventListener(spamListEventListener);
    }

}
