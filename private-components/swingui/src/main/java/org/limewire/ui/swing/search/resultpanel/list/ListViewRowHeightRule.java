package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * Defines a set of rules for determining the row height of a search result
 * displayed in the List view.  This is essentially a factory for creating 
 * RowDisplayResult values.
 */
public interface ListViewRowHeightRule {
    enum RowDisplayConfig {
        HeadingOnly(36), HeadingAndSubheading(44), HeadingAndMetadata(44), HeadingSubHeadingAndMetadata(56);
        
        private final int height;
        RowDisplayConfig(int height) {
            this.height = height;
        }
        
        public int getRowHeight() {
            return height;
        }
    }
    
    /** Initializes this rule with a search. */
    void initializeWithSearch(String search);
    
    /**
     * Determines which combination of heading, subheading, and metadata should display
     * in the list view of the search results, given a specific VisualSearchResult.
     */
    RowDisplayResult createDisplayResult(VisualSearchResult vsr);
    
    /**
     * Creates a RowDisplayResult for the specified search result that uses
     * the specified custom row height.
     */
    RowDisplayResult createDisplayResult(VisualSearchResult vsr, int rowHeight);
    
    public static interface RowDisplayResult {
        RowDisplayConfig getConfig();
        String getHeading();
        String getSubheading();
        PropertyMatch getMetadata();
        int getRowHeight();
        boolean isSpam();
        boolean isStale(VisualSearchResult vsr);
    }
    
    public static interface PropertyMatch {
        String getKey();
        String getHighlightedValue();
    }
}
