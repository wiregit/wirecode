package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.components.LimeProgressBarFactory;
import org.limewire.ui.swing.table.LimeSingleColumnTableFormat;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.CategoryIconManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;

public class UploadTable extends MouseableTable {

    public UploadTable(EventList<UploadItem> swingThreadSafeUploads, CategoryIconManager categoryIconManager, LimeProgressBarFactory progressBarFactory) {
        setModel(new EventTableModel<UploadItem>(swingThreadSafeUploads, new LimeSingleColumnTableFormat<UploadItem>()));
        TableRendererEditor editor = new UploadTableRendererEditor(categoryIconManager, progressBarFactory);
        getColumn(0).setCellEditor(editor);
        getColumn(0).setCellRenderer(new UploadTableRendererEditor(categoryIconManager, progressBarFactory));
        setRowHeight(editor.getPreferredSize().height);
    }

}
