package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Removes the selected file(s) from the library. 
 */
class RemoveFromLibraryAction extends AbstractAction {
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final LibraryManager libraryManager;
    
    @Inject
    public RemoveFromLibraryAction(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems, 
            LibraryManager libraryManager) {
        super(I18n.tr("Remove from Library"));
        
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.libraryManager = libraryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {       
        File currentSong = PlayerUtils.getCurrentSongFile();
        List<LocalFileItem> items = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
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