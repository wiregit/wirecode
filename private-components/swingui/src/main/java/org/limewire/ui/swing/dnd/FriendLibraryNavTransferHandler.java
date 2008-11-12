package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.TransferHandler;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.ShareListManager;

public class FriendLibraryNavTransferHandler extends TransferHandler {

    private final Friend friend;

    private final ShareListManager shareListManager;

    public FriendLibraryNavTransferHandler(Friend friend, ShareListManager shareListManager) {
        this.friend = friend;
        this.shareListManager = shareListManager;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                || info.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }

        List<File> files = Collections.emptyList();        
        if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            Transferable t = info.getTransferable();
            try {
                files = getTransferData(t);
            } catch(Throwable failed) {
                return false;
            }
        } else {// LocalFileTransferable
            Transferable t = info.getTransferable();
            try {
                files = Arrays.asList((File[])t.getTransferData(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR));
            } catch (Throwable failed) {
                return false;
            }
        }
        
        handleFiles(files);
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<File> getTransferData(Transferable t) throws UnsupportedFlavorException,
            IOException {
        return (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
    }

    private void handleFiles(final List<File> fileList) {
        for (File file : fileList) {
            if(file.isDirectory()) {
                shareListManager.getFriendShareList(friend).addFolder(file);
            } else {
                shareListManager.getFriendShareList(friend).addFile(file);
            }
        }
    }

}
