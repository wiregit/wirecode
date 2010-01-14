package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Provider;

import ca.odell.glazedlists.FunctionList.Function;

/**
 * A function to transform a grouped search result into a visual search result.
 */
class VisualSearchResultFunction implements Function<GroupedSearchResult, VisualSearchResult> {

    private final Provider<PropertiableHeadings> propertiableHeadings;
    private final VisualSearchResultStatusListener changeListener;
    
    public VisualSearchResultFunction(
            Provider<PropertiableHeadings> propertiableHeadings,
            VisualSearchResultStatusListener changeListener) {
        this.propertiableHeadings = propertiableHeadings;
        this.changeListener = changeListener;
    }
    
    @Override
    public VisualSearchResult evaluate(GroupedSearchResult sourceValue) {
        return new VisualSearchResultImpl(sourceValue, propertiableHeadings, changeListener);
    }
}
