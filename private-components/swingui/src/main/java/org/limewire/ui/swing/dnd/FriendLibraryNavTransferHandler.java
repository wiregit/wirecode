package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.TransferHandler;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.util.BackgroundExecutorService;

public class FriendLibraryNavTransferHandler extends TransferHandler {
        
        private Friend friend;
        private LibraryManager libraryManager;
        private ShareListManager shareListManager;

        public FriendLibraryNavTransferHandler(Friend friend, LibraryManager libraryManager, ShareListManager shareListManager){
            this.friend = friend;
            this.libraryManager = libraryManager;
            this.shareListManager = shareListManager;
        }
          
        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || info.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR) ;      
       }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            Transferable t = info.getTransferable();
            try {
                handleFiles(getTransferData(t));
            } catch (Exception e) {
                e.printStackTrace();return false;
            }

        } else {// LocalFileTransferable 
            Transferable t = info.getTransferable();
            try {
                handleFiles(Arrays.asList((File[]) t.getTransferData(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR)));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    
            return true;
        }
        
        @SuppressWarnings("unchecked")
        private List<File> getTransferData(Transferable t) throws UnsupportedFlavorException, IOException{
            return (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
        }

        private void handleFiles(final List<File> fileList){
            BackgroundExecutorService.schedule(new Runnable() {
                public void run() {
                    for (File file : fileList) {
                        libraryManager.getLibraryManagedList().addFile(file);
                        shareListManager.getFriendShareList(friend).addFile(file);
                    }
                }
            });
        }

}
