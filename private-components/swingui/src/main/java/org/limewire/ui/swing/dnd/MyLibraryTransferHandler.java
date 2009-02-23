package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.SharingMatchingEditor;
import org.limewire.ui.swing.util.DNDUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class MyLibraryTransferHandler extends TransferHandler {

    private final EventSelectionModel<LocalFileItem> selectionModel;
    private final LibraryFileList libraryManagedList;
    private final ShareListManager shareListManager;
    private final SharingMatchingEditor sharingMatcherEditor;
    
    public MyLibraryTransferHandler(EventSelectionModel<LocalFileItem> selectionModel, LibraryFileList libraryManagedList, ShareListManager shareListManager, SharingMatchingEditor sharingMatcherEditor) {
        this.selectionModel = selectionModel;
        this.libraryManagedList = libraryManagedList;
        this.shareListManager = shareListManager;
        this.sharingMatcherEditor = sharingMatcherEditor;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return !info.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR) && DNDUtils.containsFileFlavors(info);
    }

    @Override
    public int getSourceActions(JComponent comp) {
        return COPY;
    }

    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }

        // Get the string that is being dropped.
        Transferable t = info.getTransferable();
        final List<File> fileList;
        try {
            fileList = Arrays.asList(DNDUtils.getFiles(t));
        } catch (Exception e) {
            return false;
        }

        for (File file : fileList) {
            // if is a folder
            if(file.isDirectory()) {
                //if not in filtered mode
                if(sharingMatcherEditor.getCurrentFriend() == null)
                    libraryManagedList.addFolder(file);
                else
                    shareListManager.getFriendShareList(sharingMatcherEditor.getCurrentFriend()).addFolder(file);
            } else {
                //if not in filtered mode
                if(sharingMatcherEditor.getCurrentFriend() == null)
                    libraryManagedList.addFile(file);
                else
                    shareListManager.getFriendShareList(sharingMatcherEditor.getCurrentFriend()).addFile(file);
            }
        }
        return true;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
        if(selectionModel != null) {
            EventList<LocalFileItem> fileList = selectionModel.getSelected();
            File[] files = new File[fileList.size()];
            for (int i = 0; i < files.length; i++) {
                files[i] = fileList.get(i).getFile();
            }
            return new LocalFileTransferable(files);
        }
        
        return null;
    }
}
