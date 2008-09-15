package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.gui.TableFormat;

public class SharingFancyIconTableFormat implements TableFormat<LocalFileItem> {
  
    public static final String[] columnLabels = new String[] {"Name", ""};
    
    @Override
    public int getColumnCount() {
        return columnLabels.length;
    }

    @Override
    public String getColumnName(int column) {
        if(column < 0 || column >= columnLabels.length)
            throw new IllegalStateException("Unknown column:" + column);

        return columnLabels[column];
    }

    @Override
    public Object getColumnValue(LocalFileItem fileItem, int column) {
        if(column == 0) return fileItem;
        else if(column == 1) return fileItem;
        
        throw new IllegalStateException("Unknown column:" + column);
    }
}
