package org.limewire.ui.swing.search.model;

import java.util.Comparator;
import java.util.List;

import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.FunctionList.AdvancedFunction;

public class BasicSearchResultsModel {
    
    private final EventList<SearchResult> allSearchResults;    
    private final GroupingList<SearchResult> groupingListUrns;    
    private final FunctionList<List<SearchResult>, VisualSearchResult> groupedUrnResults;
//    private final GroupingList<VisualSearchResult> groupingListSimilarResults;
//    private final FunctionList<List<VisualSearchResult>, VisualSearchResult> 
    
    public BasicSearchResultsModel() {
        allSearchResults = new BasicEventList<SearchResult>();        
        groupingListUrns = new GroupingList<SearchResult>(allSearchResults, new UrnComparator());
        groupedUrnResults = new FunctionList<List<SearchResult>, VisualSearchResult>(groupingListUrns, new SearchResultGrouper()); 
    }
    
    public EventList<VisualSearchResult> getVisualSearchResults() {
        return groupedUrnResults;
    }
    
    public void addSearchResult(SearchResult result) {
        allSearchResults.add(result);
    }
    
    private static class UrnComparator implements Comparator<SearchResult> {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
            return o1.getUrn().compareTo(o2.getUrn());
        }
    }
    
    private static class SearchResultGrouper implements
            AdvancedFunction<List<SearchResult>, VisualSearchResult> {
        @Override
        public void dispose(List<SearchResult> sourceValue,
                VisualSearchResult transformedValue) {
        }

        @Override
        public VisualSearchResult evaluate(List<SearchResult> sourceValue) {
            return new SearchResultAdapter(sourceValue);
        }

        @Override
        public VisualSearchResult reevaluate(List<SearchResult> sourceValue,
                VisualSearchResult transformedValue) {
            // Do nothing, the adapter will fix it for us.
            //((SearchResultAdapter) transformedValue).newSources(sourceValue);
            return transformedValue;
        }

    }
    
    

}
