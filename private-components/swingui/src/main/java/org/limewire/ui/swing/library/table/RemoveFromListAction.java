package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Removes the selected items from the selected SharedList
 */
public class RemoveFromListAction extends AbstractAction {
    private final LibraryPanel libraryPanel;
    private final LibraryNavigatorPanel libraryNavigatorPanel;
    
    @Inject
    public RemoveFromListAction(LibraryPanel libraryPanel, LibraryNavigatorPanel libraryNavigatorPanel) {
        super(I18n.tr("Remove from List"));
        
        this.libraryPanel = libraryPanel;
        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LibraryNavItem navItem = libraryNavigatorPanel.getSelectedNavItem();
        LocalFileList localFileList = navItem.getLocalFileList();
        
        File currentSong = PlayerUtils.getCurrentSongFile();
        List<LocalFileItem> items = Collections.unmodifiableList(libraryPanel.getSelectedItems());
        for(LocalFileItem item : items) {
            if(item.getFile().equals(currentSong)){
                PlayerUtils.stop();
            }
            if(!item.isIncomplete()) {
                localFileList.removeFile(item.getFile());
            }
        }
    }
}