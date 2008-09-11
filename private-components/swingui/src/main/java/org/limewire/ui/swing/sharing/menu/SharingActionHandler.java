package org.limewire.ui.swing.sharing.menu;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.FileItem.Category;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class SharingActionHandler {
    
    public static final String UNSHARE = "UNSHARE";
    public static final String LOCATE = "LOCATE";
    public static final String PROPERTIES = "PROPERTIES";
    public static final String LAUNCH = "LAUNCH";
    
    public void performAction(final String actionCommand, final LocalFileList fileList, final LocalFileItem item) {
        //TODO: move this to a background executor
        Thread t = new Thread(new Runnable(){
            public void run() {
                if(actionCommand == UNSHARE) {
                    fileList.removeFile(item.getFile());
                } else if(actionCommand == LOCATE) {
                    NativeLaunchUtils.launchExplorer(item.getFile());
                } else if(actionCommand == PROPERTIES) {
                    //TODO: need to make properties get info and launch it here
                } else if(actionCommand == LAUNCH) {
                    launch(item);
                }     
            }
        });
        t.start();
    }
    
    private void launch(LocalFileItem item) {
        if(item.getCategory() == Category.AUDIO){
            PlayerUtils.play(item.getFile());
        } else {
            NativeLaunchUtils.launchFile(item.getFile());
        }
    }
}
