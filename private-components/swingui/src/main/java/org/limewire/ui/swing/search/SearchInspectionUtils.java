package org.limewire.ui.swing.search;

import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;

public class SearchInspectionUtils {

    @InspectablePrimitive(value = "search sorts", category = DataCategory.USAGE)
    private static volatile int searchSorts;

    public static void searchSorted() {
        searchSorts++;
    }
}
