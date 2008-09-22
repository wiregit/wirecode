/**
 * 
 */
package org.limewire.ui.swing.search.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.FunctionList.AdvancedFunction;

class UrnResultGrouper implements
        AdvancedFunction<List<SearchResult>, VisualSearchResult> {

    private final AtomicInteger resultCount;

    public UrnResultGrouper(AtomicInteger resultCount) {
        this.resultCount = resultCount;
    }

    @Override
    public void dispose(List<SearchResult> sourceValue, VisualSearchResult transformedValue) {
        resultCount.addAndGet(-transformedValue.getSources().size());
    }

    @Override
    public VisualSearchResult evaluate(List<SearchResult> sourceValue) {
        BasicSearchResultAdapter adapter = new BasicSearchResultAdapter(sourceValue);
        resultCount.addAndGet(adapter.getSources().size());
        return adapter;
    }

    @Override
    public VisualSearchResult reevaluate(List<SearchResult> sourceValue,
            VisualSearchResult transformedValue) {
        resultCount.addAndGet(-transformedValue.getSources().size());
        ((BasicSearchResultAdapter) transformedValue).update();
        resultCount.addAndGet(transformedValue.getSources().size());
        return transformedValue;
    }
}