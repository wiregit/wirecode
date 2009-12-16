package org.limewire.ui.swing.player;

import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;

public class PlayerInspectionUtils {
    
    @SuppressWarnings("unused")
    @InspectablePrimitive(value = "player buttons used", category = DataCategory.USAGE)
    private static volatile boolean arePlayerButtonsUsed = false;
    
    public static void playerUsed(){
        arePlayerButtonsUsed = true;
    }

}
