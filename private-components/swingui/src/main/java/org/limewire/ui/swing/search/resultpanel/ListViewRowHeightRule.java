package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.model.VisualSearchResult;

public interface ListViewRowHeightRule {
    enum RowDisplayConfig {
        HeadingOnly(34), HeadingAndSubheading(40), HeadingSubHeadingAndMetadata(56);
        
        private final int height;
        RowDisplayConfig(int height) {
            this.height = height;
        }
        
        public int getRowHeight() {
            return height;
        }
    }
    
    /**
     * Determines which combination of heading, subheading, and metadata should display
     * in the list view of the search results, given a specific VisualSearchResult 
     */
    RowDisplayResult getDisplayResult(VisualSearchResult vsr, String searchText);
    
    public static interface RowDisplayResult {
        String getHeading();
        String getSubheading();
        PropertyMatch getMetadata();
        RowDisplayConfig getConfig();
    }
    
    public static interface PropertyMatch {
        String getKey();
        String getHighlightedValue();
    }
}
