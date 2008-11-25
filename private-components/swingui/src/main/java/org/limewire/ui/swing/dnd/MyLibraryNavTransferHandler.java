package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.TransferHandler;

import org.limewire.core.api.download.DownLoadAction;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.DNDUtils;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

public class MyLibraryNavTransferHandler extends TransferHandler {

    private DownloadListManager downloadListManager;

    private LibraryManager libraryManager;

    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    
    public MyLibraryNavTransferHandler(DownloadListManager downloadListManager,
            LibraryManager libraryManager, SaveLocationExceptionHandler saveLocationExceptionHandler) {
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
    }

    public boolean canImport(TransferHandler.TransferSupport info) {
        return info.isDataFlavorSupported(RemoteFileTransferable.REMOTE_FILE_DATA_FLAVOR)
                || DNDUtils.containsFileFlavors(info);
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
                remoteFileList = getRemoteTransferData(t);
            } catch (UnsupportedFlavorException e1) {
                return false;
            } catch (IOException e1) {
                return false;
            }
            
            for (final RemoteFileItem file : remoteFileList) {
                try {
                    downloadListManager.addFriendDownload(file);
                } catch (SaveLocationException e) {
                    saveLocationExceptionHandler.handleSaveLocationException(new DownLoadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            downloadListManager.addFriendDownload(file, saveFile, overwrite);
                        }
                    }, e, true);
                }
            }
        } else {// LocalFile
            Transferable t = info.getTransferable();
            final List<File> fileList;
            try {
                fileList = Arrays.asList(DNDUtils.getFiles(t));
            } catch (Exception e) {
                return false;
            }
            for (File file : fileList) {
                if (file.isDirectory()) {
                    libraryManager.getLibraryManagedList().addFolder(file);
                } else {
                    libraryManager.getLibraryManagedList().addFile(file);
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<RemoteFileItem> getRemoteTransferData(Transferable t)
            throws UnsupportedFlavorException, IOException {
        return (List<RemoteFileItem>) t
                .getTransferData(RemoteFileTransferable.REMOTE_FILE_DATA_FLAVOR);
    }
}
