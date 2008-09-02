package org.limewire.ui.swing.sharing.menu;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;

public class BuddySharingActionHandler {
    
    public static final String VIEW_LIBRARY = "VIEW_LIBRARY";
    public static final String SHARE_ALL_VIDEO = "SHARE_ALL_VIDEO";
    public static final String SHARE_ALL_AUDIO = "SHARE_ALL_AUDIO";
    public static final String SHARE_ALL_IMAGE = "SHARE_ALL_IMAGE";
    public static final String UNSHARE_ALL = "UNSHARE_ALL";
    
    public void performAction(String actionCommand, FileList fileList, FileItem item) {
//        if(actionCommand == UNSHARE) {
//            fileList.removeFile(item.getFile());
//        } else if(actionCommand == LOCATE) {
//            NativeLaunchUtils.launchExplorer(item.getFile());
//        } else if(actionCommand == PROPERTIES) {
//            
//        }
    }
}
