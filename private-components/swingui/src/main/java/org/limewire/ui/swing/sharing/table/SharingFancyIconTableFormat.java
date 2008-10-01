package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.I18n;

public class SharingFancyIconTableFormat extends AbstractTableFormat<LocalFileItem> {
  
    public SharingFancyIconTableFormat() {
        super(I18n.tr("Name"), "");
    }
    
    @Override
    public Object getColumnValue(LocalFileItem fileItem, int column) {
        if(column == 0) return fileItem;
        else if(column == 1) return fileItem;
        
        throw new IllegalStateException("Unknown column:" + column);
    }
}
