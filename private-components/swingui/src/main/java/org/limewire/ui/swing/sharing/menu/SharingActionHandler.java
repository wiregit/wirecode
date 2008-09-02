package org.limewire.ui.swing.sharing.menu;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class SharingActionHandler {
    
    public static final String UNSHARE = "UNSHARE";
    public static final String LOCATE = "LOCATE";
    public static final String PROPERTIES = "PROPERTIES";
    
    public void performAction(String actionCommand, FileList fileList, FileItem item) {
        if(actionCommand == UNSHARE) {
            fileList.removeFile(item.getFile());
        } else if(actionCommand == LOCATE) {
            NativeLaunchUtils.launchExplorer(item.getFile());
        } else if(actionCommand == PROPERTIES) {
            
        }
    }
}
