package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Opens the file info view for the given file. 
 */
class ViewFileInfoAction extends AbstractAction {
    private final LibraryPanel libraryPanel;
    private final FileInfoDialogFactory fileInfoFactory;
    
    @Inject
    public ViewFileInfoAction(LibraryPanel libraryPanel, 
            FileInfoDialogFactory fileInfoFactory) {
        super(I18n.tr("View File Info..."));
        
        this.libraryPanel = libraryPanel;
        this.fileInfoFactory = fileInfoFactory;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> localFileItems = libraryPanel.getSelectedItems();
        if(localFileItems.size() > 0)
            fileInfoFactory.createFileInfoDialog(localFileItems.get(0), FileInfoType.LOCAL_FILE);
    }
}