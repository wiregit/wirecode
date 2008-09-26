package org.limewire.ui.swing.search;

import org.limewire.core.api.spam.SpamManager;
import org.limewire.ui.swing.search.model.BasicSearchResultsModel;
import org.limewire.ui.swing.search.model.GroupingListEventListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.SimilarResultsDetectorFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchResultsModelFactory {
    private final SpamManager spamManager;
    private final SimilarResultsDetectorFactory similarResultsDetectorFactory;

    @Inject
    public SearchResultsModelFactory(SimilarResultsDetectorFactory similarResultsDetectorFactory, SpamManager spamManager) {
        this.similarResultsDetectorFactory = similarResultsDetectorFactory;
        this.spamManager = spamManager;
    }

    public SearchResultsModel createSearchResultsModel() {
        SearchResultsModel searchResultsModel = new BasicSearchResultsModel();
        
        EventList<VisualSearchResult> visualSearchResults = searchResultsModel.getGroupedSearchResults();
        
        GroupingListEventListener groupingListEventListener = new GroupingListEventListener(similarResultsDetectorFactory.newSimilarResultsDetector());
        visualSearchResults.addListEventListener(groupingListEventListener);
        
        SpamListEventListener spamListEventListener = new SpamListEventListener(spamManager);
        visualSearchResults.addListEventListener(spamListEventListener);
        
        return searchResultsModel;
    }

}
