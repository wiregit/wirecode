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
 * Tries to safely launch the given file.
 * If it is an allowed file type it will be launched, 
 * otherwise explorer will be opened to the files location
 */
class LaunchFileAction extends AbstractAction {
    private final LibraryPanel libraryPanel;
    
    @Inject
    public LaunchFileAction(LibraryPanel libraryPanel) {
        super(I18n.tr("Play/Open/View"));

        this.libraryPanel = libraryPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> localFileItems = libraryPanel.getSelectedItems();
        if(localFileItems.size() > 0)
            NativeLaunchUtils.safeLaunchFile(localFileItems.get(0).getFile());
    }
}