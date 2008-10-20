package org.limewire.ui.swing.sharing.menu;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class SharingActionHandler {
    
    public static final String UNSHARE = "UNSHARE";
    public static final String LOCATE = "LOCATE";
    public static final String PROPERTIES = "PROPERTIES";
    public static final String LAUNCH = "LAUNCH";
    
    public void performAction(final String actionCommand, final LocalFileList fileList, final LocalFileItem item) {
        BackgroundExecutorService.schedule(new Runnable(){
            public void run() {
                if(actionCommand == UNSHARE) {
                    fileList.removeFile(item.getFile());
                } else if(actionCommand == LOCATE) {
                    NativeLaunchUtils.launchExplorer(item.getFile());
                } else if(actionCommand == PROPERTIES) {
                    //TODO: implement properties menu
                    throw new UnsupportedOperationException("TODO: implement properties get info");
                } else if(actionCommand == LAUNCH) {
                    launch(item);
                }     
            }
        });
    }
    
    private void launch(LocalFileItem item) {
        if(item.getCategory() == Category.AUDIO){
            PlayerUtils.play(item.getFile());
        } else {
            NativeLaunchUtils.launchFile(item.getFile());
        }
    }
}
