package org.limewire.ui.swing.search.filter;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * A matcher used to filter a search result by length.
 */
public class LengthMatcher implements Matcher<VisualSearchResult> {
    private final long minLength;
    private final long maxLength;
    
    /**
     * Constructs a LengthMatcher for the specified length range.
     */
    public LengthMatcher(long minLength, long maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    /**
     * Returns true if the specified search result is within the length range.
     */
    @Override
    public boolean matches(VisualSearchResult vsr) {
        Object value = vsr.getProperty(FilePropertyKey.LENGTH);
        if (value instanceof Long) {
            long length = ((Long) value).longValue();
            return ((minLength <= length) && (length <= maxLength));
        } else {
            return false;
        }
    }
}
