package org.limewire.ui.swing.search.filter;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * A matcher used to filter a search result by bit rate.
 */
class BitRateMatcher implements Matcher<VisualSearchResult> {
    private final long bitRate;
    
    /**
     * Constructs a BitRateMatcher for the specified bit rate.
     */
    public BitRateMatcher(long bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * Returns true if the specified search result matches or exceeds the bit 
     * rate.
     */
    @Override
    public boolean matches(VisualSearchResult vsr) {
        if (bitRate == 0) return true;
        
        Object rate = vsr.getProperty(FilePropertyKey.BITRATE);
        if (rate instanceof Long) {
            return (((Long) rate).longValue() >= bitRate);
        } else {
            return false;
        }
    }
}
