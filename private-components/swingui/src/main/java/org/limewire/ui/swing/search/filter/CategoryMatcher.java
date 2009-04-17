package org.limewire.ui.swing.search.filter;

import org.limewire.core.api.Category;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * A matcher used to filter a search result by category.
 */
class CategoryMatcher implements Matcher<VisualSearchResult> {
    private final Category category;
    
    /**
     * Constructs a CategoryMatcher for the specified category.
     */
    public CategoryMatcher(Category category) {
        this.category = category;
    }

    /**
     * Returns true if the specified search result matches the category.
     */
    @Override
    public boolean matches(VisualSearchResult vsr) {
        return (vsr.getCategory() == category);
    }
}
