package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.BackgroundExecutorService;
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
        List<LocalFileItem> selected = selectedLocalFileItems.get();
        
        int confirmation = FocusJOptionPane.showConfirmDialog(null, getMessage(selected), I18n.tr("Remove Files"), JOptionPane.OK_CANCEL_OPTION); 
        if (confirmation == JOptionPane.OK_OPTION) {
            final List<File> toRemove = new ArrayList<File>(selected.size());
            for(LocalFileItem item : selected) {
                if(item.getFile().equals(currentSong)){
                    PlayerUtils.stop();
                }
                if(!item.isIncomplete()) {
                    toRemove.add(item.getFile());
                }
            }
            
            if(!toRemove.isEmpty()) {
                BackgroundExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        for(File file : toRemove) {
                            libraryManager.getLibraryManagedList().removeFile(file);
                        }
                    }
                });
            }
        }
    }
    
    private String getMessage(List<LocalFileItem> list) {
        if(list.size() == 1) {
            return I18n.tr("Remove {0} from Library? This will unshare this file with everyone.", list.get(0).getFileName());
        } else {
            return I18n.tr("Remove {0} files from Library? This will unshare these files with everyone.", list.size());
        }
    }
}