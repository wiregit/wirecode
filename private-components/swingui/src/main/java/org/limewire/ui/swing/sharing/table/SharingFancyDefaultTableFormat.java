package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Default information to display in a table for what is being shared. 
 * Defaultly only the file name is displayed.
 */
public class SharingFancyDefaultTableFormat implements TableFormat<LocalFileItem> {
  
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
    public Object getColumnValue(LocalFileItem baseObject, int column) {
        if(column == 0) return baseObject.getName();
        else if(column == 1) return baseObject;
        
        throw new IllegalStateException("Unknown column:" + column);
    }
}
