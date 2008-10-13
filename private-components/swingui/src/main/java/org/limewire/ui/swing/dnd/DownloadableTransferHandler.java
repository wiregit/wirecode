package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import javax.swing.TransferHandler;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.BackgroundExecutorService;

/**
 * For use with RemoteFileTransferable
 */
public class DownloadableTransferHandler extends TransferHandler{
    
    private DownloadListManager downloadListManager;
    
    public DownloadableTransferHandler(DownloadListManager downloadListManager){
        this.downloadListManager = downloadListManager;
    }
    
    public boolean canImport(TransferHandler.TransferSupport info) {
        return info.isDataFlavorSupported(RemoteFileTransferable.REMOTE_FILE_DATA_FLAVOR);
    }
    
    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }
        Transferable t = info.getTransferable();
        final List<RemoteFileItem> remoteFileList;
            try {
                remoteFileList = getTransferData(t);
            } catch (UnsupportedFlavorException e1) {
                return false;
            } catch (IOException e1) {
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

        return true;
    }
    
    @SuppressWarnings("unchecked")
    private List<RemoteFileItem> getTransferData(Transferable t) throws UnsupportedFlavorException, IOException{
        return (List<RemoteFileItem>) t.getTransferData(RemoteFileTransferable.REMOTE_FILE_DATA_FLAVOR);
    }

}
