package org.limewire.ui.swing.search.model;

import java.util.List;

import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.FunctionList.Function;

public class BasicSearchResultsModel {

    private final EventList<SearchResult> allSearchResults;

    private final GroupingList<SearchResult> groupingListSimilarResults;

    private final FunctionList<List<SearchResult>, VisualSearchResult> groupedSimilarResults;

    public BasicSearchResultsModel() {
        allSearchResults = new BasicEventList<SearchResult>();

        groupingListSimilarResults = new GroupingList<SearchResult>(allSearchResults,
                new GroupingComparator(new ChainedSearchResultComparator(new UrnComparator(),
                        new NamesMatchComparator())));

        groupedSimilarResults = new FunctionList<List<SearchResult>, VisualSearchResult>(
                groupingListSimilarResults, new SimilarResultGrouper());

    }

    public int getResultCount() {
        // TODO add counting back in
        return 0;
    }

    public EventList<VisualSearchResult> getVisualSearchResults() {
        return groupedSimilarResults;
    }

    public void addSearchResult(SearchResult result) {
        allSearchResults.add(result);
    }

    public void removeSearchResult(SearchResult result) {
        allSearchResults.remove(result);
    }

    private static class SimilarResultGrouper implements
            Function<List<SearchResult>, VisualSearchResult> {
        @Override
        public VisualSearchResult evaluate(List<SearchResult> sourceValue) {
            return new GroupingSearchResultAdapter(sourceValue);
        }

    }
}