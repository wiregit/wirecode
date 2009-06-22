/**
 * 
 */
package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;

/**
 * Locates the selected file on disk.
 */
class LocateFileAction extends AbstractAction {
    
    private final LibraryPanel libraryPanel;
        
    @Inject
    public LocateFileAction(LibraryPanel libraryPanel) {
        super(I18n.tr("Locate on Disk"));

        this.libraryPanel = libraryPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> items = libraryPanel.getSelectedItems();
        if(items.size() > 0) {
            LocalFileItem item = libraryPanel.getSelectedItems().get(0);
            NativeLaunchUtils.launchExplorer(item.getFile());
        }
    }
}