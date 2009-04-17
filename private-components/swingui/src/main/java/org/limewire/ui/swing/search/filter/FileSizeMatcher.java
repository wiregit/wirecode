package org.limewire.ui.swing.search.filter;

import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * A matcher used to filter a search result by file size.
 */
class FileSizeMatcher implements Matcher<VisualSearchResult> {
    private final long minSize;
    private final long maxSize;
    
    /**
     * Constructs a FileSizeMatcher for the specified file size range.
     */
    public FileSizeMatcher(long minSize, long maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    /**
     * Returns true if the specified search result is within the file size 
     * range.
     */
    @Override
    public boolean matches(VisualSearchResult vsr) {
        long size = vsr.getSize();
        return ((minSize <= size) && (size <= maxSize));
    }
}
