package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Opens the file info view for the given file. 
 */
public class ViewFileInfoAction extends AbstractAction {

//    private final LocalFileItem localFileItem;
//    private final FileInfoDialogFactory fileInfoFactory;
    
    @Inject
    public ViewFileInfoAction(){//LocalFileItem localFileItem, FileInfoDialogFactory fileInfoFactory) {
        super(I18n.tr("View File Info..."));
//        this.localFileItem = localFileItem;
//        this.fileInfoFactory = fileInfoFactory;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        fileInfoFactory.createFileInfoDialog(localFileItem, FileInfoType.LOCAL_FILE);
    }
}