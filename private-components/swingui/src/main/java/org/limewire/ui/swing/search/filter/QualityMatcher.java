package org.limewire.ui.swing.search.filter;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * A matcher used to filter a search result by quality.
 */
class QualityMatcher implements Matcher<VisualSearchResult> {
    private final long quality;
    
    /**
     * Constructs a QualityMatcher for the specified quality.
     */
    public QualityMatcher(long quality) {
        this.quality = quality;
    }

    /**
     * Returns true if the specified search result matches or exceeds the 
     * quality.
     */
    @Override
    public boolean matches(VisualSearchResult vsr) {
        if (quality == 0) return true;
        if (vsr.isSpam()) return false;
        
        Object value = vsr.getProperty(FilePropertyKey.QUALITY);
        if (value instanceof Long) {
            return (((Long) value).longValue() >= quality);
        } else {
            return false;
        }
    }
}
