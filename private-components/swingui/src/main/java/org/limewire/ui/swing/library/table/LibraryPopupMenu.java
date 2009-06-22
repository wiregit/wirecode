package org.limewire.ui.swing.library.table;

import java.util.List;

import javax.swing.JPopupMenu;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.player.PlayerUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryPopupMenu extends JPopupMenu {

    //TODO: a lot of these actions are not correct anymore
    //TODO: a lot of thse don't need to be providers
    private final LibraryPanel libraryPanel;
    private final Provider<PlayAction> playAction;
    private final Provider<LaunchFileAction> launchAction;
    private final Provider<LocateFileAction> locateAction;
    private final Provider<RemoveFromLibraryAction> removeAction;
    private final Provider<DeleteAction> deleteAction;
    private final Provider<ViewFileInfoAction> fileInfoAction;
    
    @Inject
    public LibraryPopupMenu(LibraryPanel libraryPanel, 
            Provider<LaunchFileAction> launchAction, Provider<LocateFileAction> locateAction, 
            Provider<PlayAction> playAction, Provider<RemoveFromLibraryAction> removeAction, 
            Provider<DeleteAction> deleteAction, Provider<ViewFileInfoAction> fileInfoAction) {
        this.libraryPanel = libraryPanel;
        this.launchAction = launchAction;
        this.locateAction = locateAction;
        this.playAction = playAction;
        this.removeAction = removeAction;
        this.deleteAction = deleteAction;
        this.fileInfoAction = fileInfoAction;
        
        init();
    }
    
    private void init() {
        //TODO: check for multi-file select
        List<LocalFileItem> localFileItem = libraryPanel.getSelectedItems();
        // if single selection
        if(localFileItem.size() == 1) {
            add(launchAction.get());
            add(playAction.get()).setEnabled(PlayerUtils.isPlayableFile(localFileItem.get(0).getFile()));
            addSeparator();
            add(locateAction.get());
            add(removeAction.get());
            add(deleteAction.get());
            addSeparator();
            add(fileInfoAction.get());
        } else {
            add(removeAction.get());
            add(deleteAction.get());
        }
    }
}
