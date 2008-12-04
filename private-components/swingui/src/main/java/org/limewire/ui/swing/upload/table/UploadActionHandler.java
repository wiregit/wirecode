package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;

public class UploadActionHandler {

    public final static String PLAY_COMMAND = "play";
    public final static String CANCEL_COMMAND = "cancel";
    public final static String LIBRARY_COMMAND = "library";
    public final static String LAUNCH_COMMAND = "launch";
    public final static String REMOVE_COMMAND = "remove";
    public final static String LOCATE_COMMAND = "locate";
    public final static String PROPERTIES_COMMAND = "properties";
    
    
    private EventList<UploadItem> uploadItems;
    private PropertiesFactory<UploadItem> propertiesFactory;
    
    public UploadActionHandler(EventList<UploadItem> uploadItems, PropertiesFactory<UploadItem> propertiesFactory){
        this.uploadItems = uploadItems;
        this.propertiesFactory = propertiesFactory;
    }

    public void performAction(final String actionCommmand, final UploadItem item){
        if (actionCommmand == CANCEL_COMMAND) {
            item.cancel();
        } else if (actionCommmand == LOCATE_COMMAND){
            NativeLaunchUtils.launchExplorer(item.getFile());
        } else if (actionCommmand == PROPERTIES_COMMAND){
            propertiesFactory.newProperties().showProperties(item);
        } else if (actionCommmand == REMOVE_COMMAND){
            uploadItems.remove(item);
        } else if (actionCommmand == LIBRARY_COMMAND){
            throw new RuntimeException("Implement Me: jump to file in library");
        } else if (actionCommmand == LAUNCH_COMMAND){
            NativeLaunchUtils.launchFile(item.getFile());
        } else if (actionCommmand == PLAY_COMMAND){
            if(PlayerUtils.isPlayableFile(item.getFile())){
                PlayerUtils.play(item.getFile());
            } else {
                NativeLaunchUtils.launchFile(item.getFile());
            }
        }
    }
}
