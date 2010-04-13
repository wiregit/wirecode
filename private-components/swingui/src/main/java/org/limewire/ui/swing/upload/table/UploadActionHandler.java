package org.limewire.ui.swing.upload.table;

import javax.swing.JDialog;

import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;

/**
 * Controller for user-initiated actions on an upload item.
 */
class UploadActionHandler {

    public final static String PLAY_COMMAND = "play";
    public final static String PAUSE_COMMAND = "pause";
    public final static String RESUME_COMMAND = "resume";
    public final static String CANCEL_COMMAND = "cancel";
    public final static String LIBRARY_COMMAND = "library";
    public final static String LAUNCH_COMMAND = "launch";
    public final static String REMOVE_COMMAND = "remove";
    public final static String LOCATE_ON_DISK_COMMAND = "locate";
    public final static String PROPERTIES_COMMAND = "properties";
    
    
    private final UploadMediator uploadMediator;
    private final FileInfoDialogFactory fileInfoFactory;
    private final LibraryMediator libraryMediator;
    private final CategoryManager categoryManager;
    
    @Inject UploadActionHandler(UploadMediator uploadMediator,
            LibraryMediator libraryMediator,
            FileInfoDialogFactory fileInfoFactory, 
            CategoryManager categoryManager) {
        this.uploadMediator = uploadMediator;
        this.libraryMediator = libraryMediator;
        this.fileInfoFactory = fileInfoFactory;
        this.categoryManager = categoryManager;
    }

    public void performAction(final String actionCommmand, final UploadItem item){
        if (actionCommmand == CANCEL_COMMAND) {
            uploadMediator.cancel(item, true);
        } else if (actionCommmand == LOCATE_ON_DISK_COMMAND){
            NativeLaunchUtils.launchExplorer(item.getFile());
        } else if (actionCommmand == PROPERTIES_COMMAND){
            JDialog dialog = fileInfoFactory.createFileInfoDialog(item, FileInfoType.UPLOADING_FILE);
            dialog.setVisible(true);
        } else if (actionCommmand == REMOVE_COMMAND){
            uploadMediator.remove(item);
        } else if (actionCommmand == LIBRARY_COMMAND){
            libraryMediator.selectInLibrary(item.getFile());
        } else if (actionCommmand == LAUNCH_COMMAND){
            PlayerUtils.playOrLaunch(item.getFile(), categoryManager);
        } else if (actionCommmand == PLAY_COMMAND){
            PlayerUtils.playOrLaunch(item.getFile(), categoryManager);
        } else if (actionCommmand == PAUSE_COMMAND) {
            item.pause();
        } else if (actionCommmand == RESUME_COMMAND) {
            item.resume();
        }
    }
}
