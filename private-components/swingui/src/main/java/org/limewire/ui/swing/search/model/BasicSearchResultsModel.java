package org.limewire.ui.swing.search.model;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.limewire.core.api.search.SearchResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.FunctionList.AdvancedFunction;

public class BasicSearchResultsModel implements SearchResultsModel {
    private final Log LOG = LogFactory.getLog(getClass());

    private final EventList<SearchResult> allSearchResults;

    private final AtomicInteger resultCount = new AtomicInteger();

    private ObservableElementList<VisualSearchResult> observableList;

    private FunctionList<List<SearchResult>, VisualSearchResult> groupedUrnResults;

    public BasicSearchResultsModel() {
        allSearchResults = new BasicEventList<SearchResult>();
        GroupingList<SearchResult> groupingListUrns = new GroupingList<SearchResult>(
                allSearchResults, new UrnComparator());
        groupedUrnResults = new FunctionList<List<SearchResult>, VisualSearchResult>(
                groupingListUrns, new SearchResultGrouper(resultCount));
        observableList = new ObservableElementList<VisualSearchResult>(groupedUrnResults,
                GlazedLists.beanConnector(VisualSearchResult.class));
    }

    @Override
    public int getResultCount() {
        return resultCount.get();
    }

    @Override
    public EventList<VisualSearchResult> getObservableSearchResults() {
        return observableList;
    }

    @Override
    public EventList<VisualSearchResult> getGroupedSearchResults() {
        return groupedUrnResults;
    }

    @Override
    public void addSearchResult(SearchResult result) {
        LOG.debugf("Adding result urn: {0} EDT: {1}", result.getUrn(), SwingUtilities
                .isEventDispatchThread());
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