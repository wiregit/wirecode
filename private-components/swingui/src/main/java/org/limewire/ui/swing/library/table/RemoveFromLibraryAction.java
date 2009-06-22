package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Removes the selected file(s) from the library. 
 */
class RemoveFromLibraryAction extends AbstractAction {
    private final LibraryPanel libraryPanel;
    private final LibraryManager libraryManager;
    
    @Inject
    public RemoveFromLibraryAction(LibraryPanel libraryPanel, LibraryManager libraryManager) {
        super(I18n.tr("Remove from Library"));
        
        this.libraryPanel = libraryPanel;
        this.libraryManager = libraryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {       
        File currentSong = PlayerUtils.getCurrentSongFile();
        List<LocalFileItem> items = Collections.unmodifiableList(libraryPanel.getSelectedItems());
        for(LocalFileItem item : items) {
            if(item.getFile().equals(currentSong)){
                PlayerUtils.stop();
            }
            if(!item.isIncomplete()) {
                libraryManager.getLibraryManagedList().removeFile(item.getFile());
            }
        }
    }
}