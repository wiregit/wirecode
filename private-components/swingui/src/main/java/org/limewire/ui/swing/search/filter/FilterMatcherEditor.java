package org.limewire.ui.swing.search.filter;

import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * A MatcherEditor used to filter search results.  FilterMatcherEditor accepts
 * an arbitrary Matcher.
 */
class FilterMatcherEditor extends AbstractMatcherEditor<VisualSearchResult> {
    
    /**
     * Constructs a FilterMatcherEditor with the default Matcher.
     */
    public FilterMatcherEditor() {
    }

    /**
     * Sets the specified matcher, and notifies listeners that the matcher has
     * changed.  If <code>matcher</code> is null, then the default Matcher is
     * applied.
     */
    public void setMatcher(Matcher<VisualSearchResult> matcher) {
        if (matcher != null) {
            fireChanged(matcher);
        } else {
            fireMatchAll();
        }
    }
}