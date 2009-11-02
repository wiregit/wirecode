package org.limewire.ui.swing.library.table;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryPopupMenu extends JPopupMenu {

    private final Provider<List<File>> selectedFiles;
    private final Provider<LocalFileList> selectedLocalFileList;
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final SharedFileListManager sharedFileListManager;
    private final LibraryNavigatorPanel libraryNavigatorPanel;
    private final ListMenuFactory listMenuFactory;
    private final Provider<LaunchFileAction> launchAction;
    private final Provider<RenameFileAction> renameFileAction;
    private final Provider<LocateFileAction> locateAction;
    private final Provider<RemoveFromListAction> removeListAction;
    private final Provider<RemoveFromAllListAction> removeFromAllListAction;
    private final RemoveFromLibraryAction removeAction;
    private final DeleteAction deleteAction;
    private final Provider<ViewFileInfoAction> fileInfoAction;
    
    @Inject
    public LibraryPopupMenu(
            @LibrarySelected Provider<List<File>> selectedFiles,
            @LibrarySelected Provider<LocalFileList> selectedLocalFileList,
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems, 
            SharedFileListManager sharedFileListManager,
            LibraryNavigatorPanel libraryNavigatorPanel,
            ListMenuFactory listMenuFactory,
            Provider<RemoveFromAllListAction> removeFromAllListAction,
            Provider<LaunchFileAction> launchAction, Provider<LocateFileAction> locateAction, 
            RemoveFromLibraryAction removeAction, Provider<RenameFileAction> renameFileAction,
            Provider<RemoveFromListAction> removeListAction, 
            DeleteAction deleteAction, Provider<ViewFileInfoAction> fileInfoAction) {
        this.selectedFiles = selectedFiles;
        this.selectedLocalFileList = selectedLocalFileList;
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.sharedFileListManager = sharedFileListManager;
        this.libraryNavigatorPanel = libraryNavigatorPanel;
        this.listMenuFactory = listMenuFactory;
        this.launchAction = launchAction;
        this.renameFileAction = renameFileAction;
        this.locateAction = locateAction;
        this.removeListAction = removeListAction;
        this.removeFromAllListAction = removeFromAllListAction;
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
            addSeparator();
            
            add(listMenuFactory.createAddToListMenu(selectedFiles, selectedLocalFileList));
            if(libraryNavigatorPanel.getSelectedNavItem().getType() != NavType.LIBRARY) {
                add(listMenuFactory.createShowInListMenu(selectedFiles, selectedLocalFileList));
                add(removeListAction.get());
            } else {
                add(listMenuFactory.createShowInListMenu(selectedFiles, selectedLocalFileList));
                add(removeFromAllListAction.get()).setEnabled(existsInAnyList(localFileItem.get(0).getFile()));
            }
            addSeparator();
            
            add(renameFileAction.get()).setEnabled(!localFileItem.get(0).isIncomplete());
            add(locateAction.get());
            add(removeAction);
            add(deleteAction);
            addSeparator();
            add(fileInfoAction.get());
        } else {
            add(listMenuFactory.createAddToListMenu(selectedFiles, selectedLocalFileList));
            if(libraryNavigatorPanel.getSelectedNavItem().getType() != NavType.LIBRARY) {
                add(removeListAction.get());
            } else {
                add(removeFromAllListAction.get());
            }
            addSeparator();
            add(removeAction);
            add(deleteAction);
        }
    }
    
    private boolean existsInAnyList(File file) {
        boolean contains = false;
        for(SharedFileList sharedFileList : sharedFileListManager.getModel()) {
            if(sharedFileList.contains(file)) {
                contains = true;
                break;
            }
        }
        return contains;
    }
}
