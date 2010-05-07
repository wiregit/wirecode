package org.limewire.ui.swing.library.table;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.LibrarySelected;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryPopupMenu extends JPopupMenu {

    private final Provider<LocalFileList> selectedLocalFileList;
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final Provider<LaunchFileAction> launchAction;
    private final Provider<RenameFileAction> renameFileAction;
    private final Provider<LocateFileAction> locateAction;
    private final Provider<RemoveFromListAction> removeFromListAction;
    private final Provider<RemoveFromLibraryAction> removeFromLibraryAction;
    private final DeleteAction deleteAction;
    private final Provider<ViewFileInfoAction> fileInfoAction;
    
    @Inject
    public LibraryPopupMenu(
            @LibrarySelected Provider<LocalFileList> selectedLocalFileList,
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems,
            Provider<LaunchFileAction> launchAction,
            Provider<RenameFileAction> renameFileAction,
            Provider<LocateFileAction> locateAction,
            Provider<RemoveFromListAction> removeFromListAction,
            Provider<RemoveFromLibraryAction> removeFromLibraryAction,
            DeleteAction deleteAction,
            Provider<ViewFileInfoAction> fileInfoAction) {
        this.selectedLocalFileList = selectedLocalFileList;
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.launchAction = launchAction;
        this.renameFileAction = renameFileAction;
        this.locateAction = locateAction;
        this.removeFromListAction = removeFromListAction;
        this.removeFromLibraryAction = removeFromLibraryAction;
        this.deleteAction = deleteAction;
        this.fileInfoAction = fileInfoAction;
        
        init();
    }
    
    private void init() {
        if(selectedLocalFileList.get() instanceof LibraryFileList) {
            initLibrary();
        } else {
            initList();
        }
    }
    
    private void initLibrary() {
        List<LocalFileItem> localFileItem = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        // if single selection
        if(localFileItem.size() == 1) {
            add(launchAction.get());
            addSeparator();            
            add(renameFileAction.get()).setEnabled(!localFileItem.get(0).isIncomplete());
            add(locateAction.get());
            addSeparator();
            add(removeFromLibraryAction.get());
            add(deleteAction);
            addSeparator();
            add(fileInfoAction.get());
        } else {
            add(removeFromLibraryAction.get());
            add(deleteAction);
        }
    }
    
    private void initList() {
        List<LocalFileItem> localFileItem = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        // if single selection
        if(localFileItem.size() == 1) {
            add(launchAction.get());
            addSeparator();            
            add(renameFileAction.get()).setEnabled(!localFileItem.get(0).isIncomplete());
            add(locateAction.get());
            addSeparator();
            add(removeFromListAction.get());
            add(deleteAction);
            addSeparator();
            add(fileInfoAction.get());
        } else {
            add(removeFromListAction.get());
            add(deleteAction);
        }
    }
}
