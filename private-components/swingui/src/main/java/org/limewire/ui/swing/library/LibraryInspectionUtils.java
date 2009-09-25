package org.limewire.ui.swing.library;

import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;

public class LibraryInspectionUtils {

    @InspectablePrimitive(value = "files launched from library", category = DataCategory.USAGE)
    private static volatile int filesLaunched = 0;
    
    @InspectablePrimitive(value = "library category filters used", category = DataCategory.USAGE)
    private static volatile int categoryFilterUseCount = 0;
    
    @InspectablePrimitive(value = "library text filter used", category = DataCategory.USAGE)
    private static volatile int textFilterUseCount = 0;
    
    public static void fileLaunched(){
        filesLaunched++;
    }   
    
    public static void categoryFilterUsed(){
        categoryFilterUseCount++;
    } 
    
    public static void textFilterUsed(){
        textFilterUseCount++;
    }

}
