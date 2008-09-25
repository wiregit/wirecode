package org.limewire.ui.swing.search.model;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.FunctionList.AdvancedFunction;

public class BasicSearchResultsModel implements SearchResultsModel {

    private final EventList<SearchResult> allSearchResults;

    private final GroupingList<SearchResult> groupingListUrns;

    private final FunctionList<List<SearchResult>, VisualSearchResult> groupedUrnResults;

    private final AtomicInteger resultCount = new AtomicInteger();

    public BasicSearchResultsModel() {
        allSearchResults = new BasicEventList<SearchResult>();
        groupingListUrns = new GroupingList<SearchResult>(allSearchResults, new UrnComparator());
        groupedUrnResults = new FunctionList<List<SearchResult>, VisualSearchResult>(
                groupingListUrns, new SearchResultGrouper(resultCount));
    }

    @Override
    public int getResultCount() {
        return resultCount.get();
    }

    @Override
    public EventList<VisualSearchResult> getVisualSearchResults() {
        return groupedUrnResults;
    }

    @Override
    public void addSearchResult(SearchResult result) {
        allSearchResults.add(result);
    }

    @Override
    public void removeSearchResult(SearchResult result) {
        allSearchResults.remove(result);
    }

    private static class UrnComparator implements Comparator<SearchResult> {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
            return o1.getUrn().compareTo(o2.getUrn());
        }
    }

    private static class SearchResultGrouper implements
            AdvancedFunction<List<SearchResult>, VisualSearchResult> {
        private final AtomicInteger resultCount;

        public SearchResultGrouper(AtomicInteger resultCount) {
            this.resultCount = resultCount;
        }

        @Override
        public void dispose(List<SearchResult> sourceValue, VisualSearchResult transformedValue) {
            resultCount.addAndGet(-transformedValue.getSources().size());
        }

        @Override
        public VisualSearchResult evaluate(List<SearchResult> sourceValue) {
            SearchResultAdapter adapter = new SearchResultAdapter(sourceValue);
            
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