package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Provider;

/**
 * A factory to create instances of VisualSearchResult.
 */
class VisualSearchResultFactory {

    private final Provider<PropertiableHeadings> propertiableHeadings;
    private final VisualSearchResultStatusListener changeListener;
    
    /**
     * Constructs a VisualSearchResultFactory with the specified services.
     */
    public VisualSearchResultFactory(
            Provider<PropertiableHeadings> propertiableHeadings,
            VisualSearchResultStatusListener changeListener) {
        this.propertiableHeadings = propertiableHeadings;
        this.changeListener = changeListener;
    }
    
    /**
     * Creates a visual search result for the specified grouped search result.
     */
    public VisualSearchResult create(GroupedSearchResult sourceValue) {
        return new SearchResultAdapter(sourceValue, propertiableHeadings, changeListener);
    }
}
