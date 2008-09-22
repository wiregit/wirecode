package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GroupingList;

class GroupingSearchResultAdapter extends BasicSearchResultAdapter {

    private final GroupingList<SearchResult> groupingListUrns;

    private final FunctionList<List<SearchResult>, VisualSearchResult> groupedResultsUrns;

    public GroupingSearchResultAdapter(List<SearchResult> sourceValue) {
        super(new ArrayList<SearchResult>());
       
        groupingListUrns = new GroupingList<SearchResult>(getCoreResultsEventList(), new UrnComparator());
        groupedResultsUrns = new FunctionList<List<SearchResult>, VisualSearchResult>(
                groupingListUrns, new UrnResultGrouper(new AtomicInteger(0)));

        getCoreResultsEventList().addAll(sourceValue);
    }
    
    
    @Override
    public List<VisualSearchResult> getSimilarResults() {
        return groupedResultsUrns;
    }

}