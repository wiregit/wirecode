package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.util.DNDUtils;

import com.google.inject.Inject;

public class LibraryTransferHandler extends TransferHandler {

    private final LibraryManager libraryManager;
    private final LibraryNavigatorPanel navigatorPanel;
    private final SharedFileListManager sharedFileListManager;
    
    @Inject
    public LibraryTransferHandler(LibraryManager libraryManager, 
            LibraryNavigatorPanel navigatorPanel, SharedFileListManager sharedFileListManager) {
        this.libraryManager = libraryManager;
        this.navigatorPanel = navigatorPanel;
        this.sharedFileListManager = sharedFileListManager;
    }
    
    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return (!info.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR) && DNDUtils.containsFileFlavors(info));
//            || info.isDataFlavorSupported(PLAYLIST_DATA_FLAVOR);
    }

    @Override
    public int getSourceActions(JComponent comp) {
        return COPY;
    }
    
    @Override
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
        LibraryNavItem item = navigatorPanel.getSelectedNavItem();
        //TODO: this locks
        SharedFileList sharedFileList = sharedFileListManager.getSharedFileList(item.getTabID());
        
        for (File file : fileList) {
            // if is a folder
            if(file.isDirectory()) {
//          //if not in filtered mode
//          if(listChanger.getCurrentFriend() == null)
//          libraryManagedList.addFolder(file);
                if(sharedFileList != null)
                    sharedFileList.addFolder(file);
                else
                    libraryManager.getLibraryManagedList().addFolder(file);
//          else if(listChanger.getCurrentFriend().getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
//              shareListManager.getGnutellaShareList().addFolder(file);
//          else
//              shareListManager.getFriendShareList(listChanger.getCurrentFriend()).addFolder(file);
            } else {
//          //if not in filtered mode
//          if(listChanger.getCurrentFriend() == null)
//          libraryManagedList.addFile(file);
                if(sharedFileList != null)
                    sharedFileList.addFile(file);
                else
                    libraryManager.getLibraryManagedList().addFile(file);
//          else if(listChanger.getCurrentFriend().getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
//              shareListManager.getGnutellaShareList().addFile(file);
//          else
//              shareListManager.getFriendShareList(listChanger.getCurrentFriend()).addFile(file);
            }
        }
        return true;
    }
    
    @Override
    public Transferable createTransferable(JComponent comp) {
//        if(selectionModel != null) {
//            EventList<LocalFileItem> fileList = selectionModel.getSelected();
//            File[] files = new File[fileList.size()];
//            for (int i = 0; i < files.length; i++) {
//                files[i] = fileList.get(i).getFile();
//            }
//            return new LocalFileTransferable(files);
//        }
        
        return null;
    }
}
