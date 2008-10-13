package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import javax.swing.TransferHandler;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.BackgroundExecutorService;

public class MyLibraryNavTransferHandler extends TransferHandler{
    
    private DownloadListManager downloadListManager;
    private LibraryManager libraryManager;
    
    public MyLibraryNavTransferHandler(DownloadListManager downloadListManager, LibraryManager libraryManager){
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
    }
    
    public boolean canImport(TransferHandler.TransferSupport info) {
        return info.isDataFlavorSupported(RemoteFileTransferable.REMOTE_FILE_DATA_FLAVOR) || info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);      
   }
    
    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }
        if (info.isDataFlavorSupported(RemoteFileTransferable.REMOTE_FILE_DATA_FLAVOR)) {
            Transferable t = info.getTransferable();
            final List<RemoteFileItem> remoteFileList;
            try {
                remoteFileList = (List<RemoteFileItem>) t.getTransferData(RemoteFileTransferable.REMOTE_FILE_DATA_FLAVOR);
            } catch (Exception e) {
                return false;
            }
            BackgroundExecutorService.schedule(new Runnable() {
                public void run() {
                    for (RemoteFileItem file : remoteFileList) {
                        try {
                            downloadListManager.addDownload(file);
                        } catch (SaveLocationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        } else {//LocalFile
            Transferable t = info.getTransferable();
            final List<File> fileList;
            try {
                fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
            } catch (Exception e) {
                return false;
            }
            BackgroundExecutorService.schedule(new Runnable() {
                public void run() {
                    for (File file : fileList) {
                        libraryManager.getLibraryManagedList().addFile(file);
                    }
                }
            });
        }
        return true;
    }

}
