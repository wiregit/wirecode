package org.limewire.ui.swing.search;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.SearchSettings;
import org.limewire.ui.swing.search.model.BasicSearchResultsModel;
import org.limewire.ui.swing.search.model.GroupingListEventListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.SimilarResultsDetector;
import org.limewire.ui.swing.search.model.SimilarResultsDetectorFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchResultsModelFactory {
    private final SpamManager spamManager;

    private final SimilarResultsDetectorFactory similarResultsDetectorFactory;

    private final LibraryManager libraryManager;
    
    private final DownloadListManager downloadListManager;
    
    @Inject
    public SearchResultsModelFactory(SimilarResultsDetectorFactory similarResultsDetectorFactory,
            SpamManager spamManager, LibraryManager libraryManager, DownloadListManager downloadListManager) {
        this.similarResultsDetectorFactory = similarResultsDetectorFactory;
        this.spamManager = spamManager;
        this.libraryManager = libraryManager;
        this.downloadListManager = downloadListManager;
    }

    public SearchResultsModel createSearchResultsModel() {
        SearchResultsModel searchResultsModel = new BasicSearchResultsModel();

        EventList<VisualSearchResult> visualSearchResults = searchResultsModel
                .getGroupedSearchResults();

        SimilarResultsDetector similarResultsDetector = similarResultsDetectorFactory
                .newSimilarResultsDetector();

        if(SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue()) {
            GroupingListEventListener groupingListEventListener = new GroupingListEventListener(
                similarResultsDetector);
            visualSearchResults.addListEventListener(groupingListEventListener);
        }
        SpamListEventListener spamListEventListener = new SpamListEventListener(spamManager,
                similarResultsDetector);
        visualSearchResults.addListEventListener(spamListEventListener);
        
        AlreadyDownloadedListEventListener alreadyDownloadedListEventListener = new AlreadyDownloadedListEventListener(libraryManager, downloadListManager);
        visualSearchResults.addListEventListener(alreadyDownloadedListEventListener);
        

        return searchResultsModel;
    }

}
