package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.upload.UploadItem;

/**
 * Table format for the Uploads table.
 */
public class UploadTableFormat extends TransferTableFormat<UploadItem> {

    @Override
    public Class getColumnClass(int column) {
        return UploadItem.class;
    }
}
