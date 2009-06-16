package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Removes the given file from the library. 
 */
public class RemoveAction extends AbstractAction {
    private final LibraryNavigatorTable libraryNavigatorTable;
    private final LibraryTable libraryTable;
    
    @Inject
    public RemoveAction(LibraryNavigatorTable libraryNavigatorTable, LibraryTable libraryTable) {//final LocalFileItem[] fileItemArray, LibraryManager libraryManager) {
        super(I18n.tr("Remove from Library"));
        
        this.libraryNavigatorTable = libraryNavigatorTable;
        this.libraryTable = libraryTable;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LibraryNavItem navItem = libraryNavigatorTable.getSelectedItem();
        LocalFileItem fileItem = libraryTable.getSelectedItem();
        
        navItem.getLocalFileList().removeFile(fileItem.getFile());
        
        //TODO: make this function for multi select
        // 
//        File currentSong = PlayerUtils.getCurrentSongFile();
//        for (LocalFileItem fileItem : fileItemArray) {
//            if(fileItem.getFile().equals(currentSong)){
//                PlayerUtils.stop();
//            }
//            if (!fileItem.isIncomplete()) {
//                libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
//            }
//        }
    }
}