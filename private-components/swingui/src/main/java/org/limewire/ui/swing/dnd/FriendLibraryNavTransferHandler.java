package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.TransferHandler;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.util.DNDUtils;

public class FriendLibraryNavTransferHandler extends TransferHandler {

    private final Friend friend;

    private final ShareListManager shareListManager;

    public FriendLibraryNavTransferHandler(Friend friend, ShareListManager shareListManager) {
        this.friend = friend;
        this.shareListManager = shareListManager;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return DNDUtils.containsFileFlavors(info);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }

        List<File> files = Collections.emptyList();        
        if (DNDUtils.containsFileFlavors(info)) {
            Transferable t = info.getTransferable();
            try {
                files = Arrays.asList(DNDUtils.getFiles(t));
            } catch(Throwable failed) {
                return false;
            }
        } 
        
        handleFiles(files);
        return true;
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
