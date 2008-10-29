package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.properties.PropertiesFactory;
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
    private PropertiesFactory<DownloadItem> propertiesFactory;
    
    public DownloadActionHandler(EventList<DownloadItem> downloadItems, PropertiesFactory<DownloadItem> propertiesFactory){
        this.downloadItems = downloadItems;
        this.propertiesFactory = propertiesFactory;
    }

    public void performAction(final String actionCommmand, final DownloadItem item){
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
            DownloadItemUtils.launch(item);
        } else if (actionCommmand == LOCATE_COMMAND){
            NativeLaunchUtils.launchExplorer(item.getDownloadingFile());
        } else if (actionCommmand == PROPERTIES_COMMAND){
            propertiesFactory.newProperties().showProperties(item);
        } else if (actionCommmand == REMOVE_COMMAND){
            downloadItems.remove(item);
        }
    }
}
