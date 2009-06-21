 package org.limewire.ui.swing.downloads.table;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;

public class DownloadTransferable implements Transferable {
    
    public static final DataFlavor DOWNLOAD_ITEM_DATA_FLAVOR = new DataFlavor(List.class, "DownloadItem List");
    
    private final List<File> fileList;
    
    public DownloadTransferable(List<DownloadItem> downloadItemFileList){
        this.fileList = new ArrayList<File>();
        for(DownloadItem downloadItem : downloadItemFileList) {
            if(downloadItem.getState() == DownloadState.DONE) {
                fileList.add(downloadItem.getLaunchableFile());
            }
        }
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if(!flavor.equals(DOWNLOAD_ITEM_DATA_FLAVOR)){
            throw new UnsupportedFlavorException(flavor);
        }
        return fileList;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DOWNLOAD_ITEM_DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DOWNLOAD_ITEM_DATA_FLAVOR) && fileList.size() > 0;
    }

}
