package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Removes the given file from the library. 
 */
public class RemoveAction extends AbstractAction {
    private final LibraryManager libraryManager;
    private final LocalFileItem[] fileItemArray;
    
    public RemoveAction(final LocalFileItem[] fileItemArray, LibraryManager libraryManager) {
        super(I18n.tr("Remove from Library"));
        this.libraryManager = libraryManager;
        this.fileItemArray = fileItemArray;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File currentSong = PlayerUtils.getCurrentSongFile();
        for (LocalFileItem fileItem : fileItemArray) {
            if(fileItem.getFile().equals(currentSong)){
                PlayerUtils.stop();
            }
            if (!fileItem.isIncomplete()) {
                libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
            }
        }
    }
}