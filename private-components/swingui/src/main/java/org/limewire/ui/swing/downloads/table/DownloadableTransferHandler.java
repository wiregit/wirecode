package org.limewire.ui.swing.downloads.table;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.download.DownloadItem;

class DownloadableTransferHandler extends TransferHandler {
    private final List<DownloadItem> selectedItems;

    public DownloadableTransferHandler(List<DownloadItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return false;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        return new DownloadTransferable(new ArrayList<DownloadItem>(selectedItems));
    }
}

