package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Removes the selected items from the selected SharedList
 */
public class RemoveFromListAction extends AbstractAction {
    private final Provider<LocalFileList> selectedLocalFileList;
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    
    @Inject
    public RemoveFromListAction(@LibrarySelected Provider<LocalFileList> selectedLocalFileList,
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems) {
        super(I18n.tr("Remove from List"));
        
        this.selectedLocalFileList = selectedLocalFileList;
        this.selectedLocalFileItems = selectedLocalFileItems;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LocalFileList localFileList = selectedLocalFileList.get();
        
        File currentSong = PlayerUtils.getCurrentSongFile();
        List<LocalFileItem> items = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
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