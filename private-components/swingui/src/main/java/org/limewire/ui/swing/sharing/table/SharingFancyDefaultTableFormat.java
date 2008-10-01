package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * Default information to display in a table for what is being shared. 
 * Only the file name is displayed by default.
 */
public class SharingFancyDefaultTableFormat extends AbstractTableFormat<LocalFileItem> {
  
    
    public SharingFancyDefaultTableFormat() {
        super(I18n.tr("Name"), "");
    }
    
    @Override
    public Object getColumnValue(LocalFileItem fileItem, int column) {
        if(column == 0) return fileItem.getName();
        else if(column == 1) return fileItem;
        
        throw new IllegalStateException("Unknown column:" + column);
    }
}
