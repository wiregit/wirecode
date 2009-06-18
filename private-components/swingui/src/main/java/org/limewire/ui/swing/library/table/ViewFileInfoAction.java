package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Opens the file info view for the given file. 
 */
class ViewFileInfoAction extends AbstractAction {

    private final Provider<LibraryTable> libraryTable;
    private final FileInfoDialogFactory fileInfoFactory;
    
    @Inject
    public ViewFileInfoAction(Provider<LibraryTable> libraryTable, 
            FileInfoDialogFactory fileInfoFactory) {
        super(I18n.tr("View File Info..."));
        
        this.libraryTable = libraryTable;
        this.fileInfoFactory = fileInfoFactory;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fileInfoFactory.createFileInfoDialog(libraryTable.get().getSelectedItem(), FileInfoType.LOCAL_FILE);
    }
}