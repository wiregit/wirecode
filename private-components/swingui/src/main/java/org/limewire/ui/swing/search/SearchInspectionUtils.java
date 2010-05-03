package org.limewire.ui.swing.search;

import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;

/**
 * Utility to collect search-related inspection data.
 */
public class SearchInspectionUtils {

    @InspectablePrimitive(value = "advanced search opened", category = DataCategory.USAGE)
    private static volatile int advancedSearchesOpened = 0;
    
    @InspectablePrimitive(value = "advanced searches executed", category = DataCategory.USAGE)
    private static volatile int advancedSearchesMade = 0;
    
    @InspectablePrimitive(value = "search sorts", category = DataCategory.USAGE)
    private static volatile int searchSorts;

    /**
     * Increment data for opening an advanced search.
     */
    public static void advancedSearchOpened() {
        advancedSearchesOpened++;
    }

    /**
     * Increment data for starting an advanced search.
     */
    public static void advancedSearchStarted() {
        advancedSearchesMade++;
    }

    /**
     * Increment data for sorting search results.
     */
    public static void searchSorted() {
        searchSorts++;
    }
}
