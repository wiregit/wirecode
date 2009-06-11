package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.download.DownloadItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

/**
 * For use with RemoteFileTransferable
 */
public class DownloadableTransferHandler extends TransferHandler {
    private final EventSelectionModel<DownloadItem> selectionModel;

    public DownloadableTransferHandler(EventSelectionModel<DownloadItem> selectionModel) {
        this.selectionModel = selectionModel;
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
        EventList<DownloadItem> selected = selectionModel.getSelected();
        return new DownloadTransferable(new ArrayList<DownloadItem>(selected));
    }
}
