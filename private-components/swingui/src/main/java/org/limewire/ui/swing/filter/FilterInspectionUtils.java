package org.limewire.ui.swing.filter;

import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;

public class FilterInspectionUtils {
    /** In session Y of user Z, do they type something into the refine box */
    @InspectablePrimitive(value = "refine box used", category = DataCategory.USAGE)
    @SuppressWarnings("unused")
    private static volatile boolean isRefineBoxUsed = false;
    

    /** In session Y of user Z, do they click on at least one filter */
    @InspectablePrimitive(value = "any filters used", category = DataCategory.USAGE)
    @SuppressWarnings("unused")
    private static volatile boolean isFilterUsed = false;
    

    /** In session Y of user Z, does a user click on "more filters"*/
    @InspectablePrimitive(value = "more filters button clicked", category = DataCategory.USAGE)
    @SuppressWarnings("unused")
    private static volatile boolean isMoreFiltersClicked = false;
    
    
    public static void filterUsed(){
        isFilterUsed = true;
    }
    
    public static void moreFiltersClicked(){
        isMoreFiltersClicked = true;
    }
    
    
    public static void refineBoxUsed(){
        isRefineBoxUsed = true;
    }

}
