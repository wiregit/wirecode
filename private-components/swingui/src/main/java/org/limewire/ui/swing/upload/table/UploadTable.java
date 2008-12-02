package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.table.LimeSingleColumnTableFormat;
import org.limewire.ui.swing.table.MouseableTable;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;

public class UploadTable extends MouseableTable {

    public UploadTable(EventList<UploadItem> swingThreadSafeUploads) {
        setModel(new EventTableModel<UploadItem>(swingThreadSafeUploads, new LimeSingleColumnTableFormat<UploadItem>()));
    }

}
