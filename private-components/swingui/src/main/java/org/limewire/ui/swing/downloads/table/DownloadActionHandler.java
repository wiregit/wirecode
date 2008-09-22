package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;

public class DownloadActionHandler {
    
    public final static String PAUSE_COMMAND = "pause";
    public final static String CANCEL_COMMAND = "cancel";
    public final static String RESUME_COMMAND = "resume";
    public final static String TRY_AGAIN_COMMAND = "try again";
    public final static String LAUNCH_COMMAND = "launch";
    public final static String PREVIEW_COMMAND = "preview";
    public final static String REMOVE_COMMAND = "remove";
    public final static String LOCATE_COMMAND = "locate";
    public final static String PROPERTIES_COMMAND = "properties";
    public final static String LINK_COMMAND = "link";
    
    private static final String ERROR_URL = "http://wiki.limewire.org/index.php?title=User_Guide_Download";
    
    private EventList<DownloadItem> downloadItems;
    
    public DownloadActionHandler(EventList<DownloadItem> downloadItems){
        this.downloadItems = downloadItems;
    }

    public void performAction(String actionCommmand, DownloadItem item){
        if (actionCommmand == CANCEL_COMMAND) {
            item.cancel();
        } else if (actionCommmand == PAUSE_COMMAND) {
            item.pause();
        } else if (actionCommmand == RESUME_COMMAND) {
            item.resume();
        } else if (actionCommmand == TRY_AGAIN_COMMAND) {
            item.resume();
        } else if (actionCommmand == LINK_COMMAND){
            NativeLaunchUtils.openURL(ERROR_URL);
        } else if (actionCommmand == PREVIEW_COMMAND || actionCommmand == LAUNCH_COMMAND){
            launch(item);
        } else if (actionCommmand == LOCATE_COMMAND){
            NativeLaunchUtils.launchExplorer(item.getFile());
        } else if (actionCommmand == PROPERTIES_COMMAND){
            //TODO properties
            throw new RuntimeException("Implement "+ actionCommmand  + " " + item.getTitle() + "!");
        } else if (actionCommmand == REMOVE_COMMAND){
            downloadItems.remove(item);
        }
    }

    private void launch(DownloadItem item) {
        if(item.getCategory() == Category.AUDIO){
            PlayerUtils.play(item.getFile());
        } else {
            NativeLaunchUtils.launchFile(item.getFile());
        }
    }
}
