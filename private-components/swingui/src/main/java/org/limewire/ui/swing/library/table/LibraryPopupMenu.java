package org.limewire.ui.swing.library.table;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.player.PlayerUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryPopupMenu extends JPopupMenu {

    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final LibraryNavigatorPanel libraryNavigatorPanel;
    private final Provider<PlayAction> playAction;
    private final Provider<LaunchFileAction> launchAction;
    private final Provider<LocateFileAction> locateAction;
    private final Provider<RemoveFromListAction> removeListAction;
    private final RemoveFromLibraryAction removeAction;
    private final DeleteAction deleteAction;
    private final Provider<ViewFileInfoAction> fileInfoAction;
    
    @Inject
    public LibraryPopupMenu(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems, 
            LibraryNavigatorPanel libraryNavigatorPanel,
            Provider<LaunchFileAction> launchAction, Provider<LocateFileAction> locateAction, 
            Provider<PlayAction> playAction, RemoveFromLibraryAction removeAction, 
            Provider<RemoveFromListAction> removeListAction,
            DeleteAction deleteAction, Provider<ViewFileInfoAction> fileInfoAction) {
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.libraryNavigatorPanel = libraryNavigatorPanel;
        this.launchAction = launchAction;
        this.locateAction = locateAction;
        this.removeListAction = removeListAction;
        this.playAction = playAction;
        this.removeAction = removeAction;
        this.deleteAction = deleteAction;
        this.fileInfoAction = fileInfoAction;
        
        init();
    }
    
    private void init() {
        List<LocalFileItem> localFileItem = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        // if single selection
        if(localFileItem.size() == 1) {
            add(launchAction.get());
            add(playAction.get()).setEnabled(PlayerUtils.isPlayableFile(localFileItem.get(0).getFile()));
            addSeparator();
            if(libraryNavigatorPanel.getSelectedNavItem().getType() != NavType.LIBRARY) {
                add(removeListAction.get());
                addSeparator();
            }
            add(locateAction.get());
            add(removeAction);
            add(deleteAction);
            addSeparator();
            add(fileInfoAction.get());
        } else {
            if(libraryNavigatorPanel.getSelectedNavItem().getType() != NavType.LIBRARY) {
                add(removeListAction.get());
                addSeparator();
            }
            add(removeAction);
            add(deleteAction);
        }
    }
}
