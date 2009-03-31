package org.limewire.ui.swing.downloads.table;

import java.util.Comparator;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.table.AbstractAdvancedTableFormat;

public class DownloadTableFormat extends AbstractAdvancedTableFormat<DownloadItem> {
    public DownloadTableFormat(){
        super("title", "progress", "message", "action");
    }

    @Override
    public Class getColumnClass(int column) {
        return DownloadItem.class;
    }

    @Override
    public Comparator getColumnComparator(int column) {
        return null;
    }

    @Override
    public Object getColumnValue(DownloadItem baseObject, int column) {
        return baseObject;
    }

   
}
