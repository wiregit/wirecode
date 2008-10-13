package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.download.DownloadItem;

public class DownloadTableRenderer implements TableCellRenderer {

    private DownloadTableCell cellComponent;  
        
    public DownloadTableRenderer(ActionListener editorListener) {
        this.cellComponent = new DownloadTableCell(editorListener);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        cellComponent.update((DownloadItem) value);
        return cellComponent;
    }
}
